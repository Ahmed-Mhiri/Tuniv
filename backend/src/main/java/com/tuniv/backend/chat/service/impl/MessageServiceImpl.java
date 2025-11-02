package com.tuniv.backend.chat.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.authorization.service.PermissionService;
import com.tuniv.backend.chat.dto.event.NewMessageEventDto;
import com.tuniv.backend.chat.dto.request.EditMessageRequest;
import com.tuniv.backend.chat.dto.request.SendMessageRequest;
import com.tuniv.backend.chat.dto.response.ChatMessageDto;
import com.tuniv.backend.chat.dto.response.MessageReactionsSummaryDto;
import com.tuniv.backend.chat.dto.response.MessageStatsDto;
import com.tuniv.backend.chat.event.MessageReadEvent;
import com.tuniv.backend.chat.mapper.mapstruct.MessageMapper;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.model.MessageStatus;
import com.tuniv.backend.chat.model.MessageType;
import com.tuniv.backend.chat.model.Reaction;
import com.tuniv.backend.chat.projection.message.MessageListProjection;
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.chat.repository.ConversationRepository;
import com.tuniv.backend.chat.repository.MessageRepository;
import com.tuniv.backend.chat.service.BulkDataFetcherService;
import com.tuniv.backend.chat.service.ChatRealtimeService;
import com.tuniv.backend.chat.service.EntityFinderService;
import com.tuniv.backend.chat.service.MessageService;
import com.tuniv.backend.chat.service.ReactionService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.shared.service.HtmlSanitizerService;
import com.tuniv.backend.user.model.User;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final PermissionService permissionService;
    private final MessageMapper messageMapper;
    private final BulkDataFetcherService bulkDataFetcherService;
    private final ChatRealtimeService chatRealtimeService;
    private final HtmlSanitizerService htmlSanitizerService;
    private final ReactionService reactionService;
    private final EntityFinderService entityFinderService;
    private final RabbitTemplate rabbitTemplate;
    private final ApplicationEventPublisher eventPublisher;

    // Constructor with all dependencies including ApplicationEventPublisher
    public MessageServiceImpl(
        MessageRepository messageRepository,
        ConversationRepository conversationRepository,
        ConversationParticipantRepository participantRepository,
        PermissionService permissionService,
        MessageMapper messageMapper,
        BulkDataFetcherService bulkDataFetcherService,
        ChatRealtimeService chatRealtimeService,
        HtmlSanitizerService htmlSanitizerService,
        ReactionService reactionService,
        EntityFinderService entityFinderService,
        RabbitTemplate rabbitTemplate,
        ApplicationEventPublisher eventPublisher
    ) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.permissionService = permissionService;
        this.messageMapper = messageMapper;
        this.bulkDataFetcherService = bulkDataFetcherService;
        this.chatRealtimeService = chatRealtimeService;
        this.htmlSanitizerService = htmlSanitizerService;
        this.reactionService = reactionService;
        this.entityFinderService = entityFinderService;
        this.rabbitTemplate = rabbitTemplate;
        this.eventPublisher = eventPublisher;
    }

    // Constants for RabbitMQ routing
    private static final String CHAT_EXCHANGE = "chat.exchange";
    private static final String NEW_MESSAGE_ROUTING_KEY = "event.message.new";

    @Override
    public ChatMessageDto sendMessage(Integer conversationId, SendMessageRequest request, UserDetailsImpl currentUser) {
        log.info("Sending message to conversation {} by user {}", conversationId, currentUser.getId());

        String sanitizedBody = htmlSanitizerService.sanitizeMessageBody(request.getBody(), true);
        request.setBody(sanitizedBody);

        // ✅ UPDATED: Use getActiveConversationOrThrow instead of getConversationOrThrow
        Conversation conversation = entityFinderService.getActiveConversationOrThrow(conversationId);
        User currentUserEntity = entityFinderService.getUserOrThrow(currentUser.getId());

        validateConversationMembership(conversation, currentUser);
        validateMessagePermissions(conversation, currentUser, "send_messages");
        validateNotMuted(conversation, currentUser);

        Message message = createMessageEntity(request, conversation, currentUserEntity);
        Message savedMessage = messageRepository.save(message);

        // Update conversation last message
        updateConversationLastMessage(conversation, savedMessage);
        updateParticipantMessageCount(conversation, currentUserEntity);

        // ✅ Publish event to RabbitMQ
        try {
            String bodyPreview = truncateMessageBody(savedMessage.getBody(), 100);
            NewMessageEventDto event = new NewMessageEventDto(
                savedMessage.getId(),
                conversationId,
                savedMessage.getSentAt(),
                savedMessage.getAuthor() != null ? savedMessage.getAuthor().getUserId() : null,
                bodyPreview
            );
            rabbitTemplate.convertAndSend(CHAT_EXCHANGE, NEW_MESSAGE_ROUTING_KEY, event);
            log.debug("Published NewMessageEvent for message {}", savedMessage.getId());
        } catch (Exception e) {
            log.error("Failed to publish NewMessageEvent for message {}: {}", savedMessage.getId(), e.getMessage(), e);
        }

        // Use bulk mapper for single message with reactions
        List<Message> singleMessageList = List.of(savedMessage);
        Map<Integer, List<Reaction>> reactionsByMessage = bulkDataFetcherService.getReactionsByMessages(singleMessageList);
        List<Reaction> messageReactions = reactionsByMessage.getOrDefault(savedMessage.getId(), List.of());

        // Use ReactionService for reaction calculation
        MessageReactionsSummaryDto reactionsSummary = reactionService.getMessageReactionsSummary(
            savedMessage.getId(), currentUser);

        ChatMessageDto messageDto = messageMapper.toChatMessageDto(savedMessage, reactionsSummary);

        chatRealtimeService.broadcastNewMessage(conversationId, messageDto);

        log.info("Message sent successfully with ID: {}", savedMessage.getId());
        return messageDto;
    }

    @Override
    public void createAndSendSystemMessage(Integer conversationId, String text) {
        log.info("Creating system message for conversation {}: {}", conversationId, text);

        // ✅ UPDATED: Use getActiveConversationOrThrow instead of getConversationOrThrow
        Conversation conversation = entityFinderService.getActiveConversationOrThrow(conversationId);

        Message systemMessage = new Message();
        systemMessage.setBody(text);
        systemMessage.setConversation(conversation);
        systemMessage.setMessageType(MessageType.SYSTEM);
        systemMessage.setSentAt(Instant.now());
        systemMessage.setAuthor(null); // System messages have no author

        if (conversation.getUniversityContext() != null) {
            systemMessage.setUniversityContext(conversation.getUniversityContext());
        }

        Message savedMessage = messageRepository.save(systemMessage);

        updateConversationLastMessage(conversation, savedMessage);

        ChatMessageDto messageDto = messageMapper.toChatMessageDto(savedMessage); // No reactions for system messages
        chatRealtimeService.broadcastNewMessage(conversationId, messageDto);

        log.info("System message created with ID: {}", savedMessage.getId());
    }

    @Override
    public ChatMessageDto editMessage(Integer messageId, EditMessageRequest request, UserDetailsImpl currentUser) {
        log.info("Editing message {} by user {}", messageId, currentUser.getId());

        String sanitizedBody = htmlSanitizerService.sanitizeMessageBody(request.getBody(), true);
        request.setBody(sanitizedBody);

        // ✅ UPDATED: Use getNonDeletedMessageOrThrow instead of getMessageOrThrow
        Message message = entityFinderService.getNonDeletedMessageOrThrow(messageId);
        validateMessageOwnershipOrPermission(message, currentUser, "edit_own_messages", "edit_any_message");

        message.setBody(request.getBody());
        message.setEdited(true);
        message.setEditedAt(Instant.now());
        message.setEditCount(message.getEditCount() + 1);

        Message updatedMessage = messageRepository.save(message);

        updateLastMessageIfNeeded(updatedMessage.getConversation().getConversationId(), messageId);

        // Use bulk mapper for reactions
        List<Message> singleMessageList = List.of(updatedMessage);
        Map<Integer, List<Reaction>> reactionsByMessage = bulkDataFetcherService.getReactionsByMessages(singleMessageList);
        List<Reaction> messageReactions = reactionsByMessage.getOrDefault(updatedMessage.getId(), List.of());

        // Use ReactionService for reaction calculation
        MessageReactionsSummaryDto reactionsSummary = reactionService.getMessageReactionsSummary(
            updatedMessage.getId(), currentUser);

        ChatMessageDto messageDto = messageMapper.toChatMessageDto(updatedMessage, reactionsSummary);

        chatRealtimeService.broadcastMessageUpdate(
            message.getConversation().getConversationId(),
            messageDto
        );

        log.info("Message {} edited successfully", messageId);
        return messageDto;
    }

    @Override
    public void deleteMessage(Integer messageId, UserDetailsImpl currentUser) {
        log.info("Deleting message {} by user {}", messageId, currentUser.getId());

        // ✅ UPDATED: Use getNonDeletedMessageOrThrow instead of getMessageOrThrow
        Message message = entityFinderService.getNonDeletedMessageOrThrow(messageId);
        Integer conversationId = message.getConversation().getConversationId();
        validateMessageOwnershipOrPermission(message, currentUser, "delete_own_messages", "delete_any_message");

        message.setDeleted(true);
        message.setDeletedAt(Instant.now());
        message.setDeletionReason("User deleted");

        messageRepository.save(message);

        chatRealtimeService.broadcastMessageDeletion(conversationId, messageId);
        updateLastMessageIfNeeded(conversationId, messageId);

        log.info("Message {} deleted successfully", messageId);
    }

    @Override
    public void permanentlyDeleteMessage(Integer messageId, UserDetailsImpl currentUser) {
        log.info("Permanently deleting message {} by user {}", messageId, currentUser.getId());

        // ✅ UPDATED: Use getMessageByIdEvenIfDeleted to find message even if already deleted
        Message message = entityFinderService.getMessageByIdEvenIfDeleted(messageId);
        validateMessagePermission(message, currentUser, "delete_any_message"); // Ensure only authorized users can permanently delete

        messageRepository.delete(message); // Hard delete

        // Optionally: Broadcast a permanent deletion event if needed by clients
        // chatRealtimeService.broadcastMessagePermanentDeletion(conversationId, messageId);

        updateLastMessageIfNeeded(message.getConversation().getConversationId(), messageId); // Update conversation last message if needed

        log.info("Message {} permanently deleted", messageId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getMessagesForConversation(Integer conversationId, UserDetailsImpl currentUser, Pageable pageable) {
        log.debug("Fetching messages for conversation {} by user {}", conversationId, currentUser.getId());

        // 1. VALIDATION
        Conversation conversation = entityFinderService.getActiveConversationOrThrow(conversationId);
        validateConversationMembership(conversation, currentUser);

        // 2. FETCH PROJECTIONS
        Page<MessageListProjection> projectionsPage = messageRepository
            .findMessageProjectionsByConversation(conversationId, pageable);

        if (projectionsPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // 3. BULK-FETCHING LOGIC
        List<MessageListProjection> projections = projectionsPage.getContent();
        List<Integer> messageIds = projections.stream()
            .map(MessageListProjection::getId)
            .collect(Collectors.toList());

        // 4. FETCH ALL REACTION DATA IN 2 QUERIES
        Map<Integer, Map<String, Long>> reactionCountsMap = reactionService
            .getReactionCountsForMessages(messageIds);
        Map<Integer, String> currentUserReactionsMap = reactionService
            .getUserReactionsForMessages(currentUser.getId(), messageIds);

        // 5. IN-MEMORY MAPPING
        List<ChatMessageDto> dtos = new ArrayList<>();
        for (MessageListProjection projection : projections) {
            ChatMessageDto dto = messageMapper.projectionToDto(projection);

            Integer messageId = projection.getId();
            Map<String, Long> reactionCounts = reactionCountsMap.getOrDefault(messageId, Map.of());
            String userReactionEmoji = currentUserReactionsMap.get(messageId);

            Map<String, Boolean> userReactions = reactionCounts.keySet().stream()
                .collect(Collectors.toMap(
                    emoji -> emoji,
                    emoji -> emoji.equals(userReactionEmoji)
                ));

            List<String> topReactions = reactionCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

            MessageReactionsSummaryDto reactionsSummary = new MessageReactionsSummaryDto();
            reactionsSummary.setMessageId(messageId);
            reactionsSummary.setReactionCounts(reactionCounts);
            reactionsSummary.setUserReactions(userReactions);
            reactionsSummary.setTopReactions(topReactions);
            reactionsSummary.setTotalReactions(
                reactionCounts.values().stream().mapToInt(Long::intValue).sum()
            );

            dto.setReactionsSummary(reactionsSummary);
            dtos.add(dto);
        }

        // 6. RETURN
        return new PageImpl<>(dtos, pageable, projectionsPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ChatMessageDto getMessage(Integer messageId, UserDetailsImpl currentUser) {
        log.debug("Fetching message {} by user {}", messageId, currentUser.getId());

        // ✅ UPDATED: Use getNonDeletedMessageOrThrow instead of getMessageOrThrow
        Message message = entityFinderService.getNonDeletedMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser);

        // Use bulk mapper for reactions
        List<Message> singleMessageList = List.of(message);
        Map<Integer, List<Reaction>> reactionsByMessage = bulkDataFetcherService.getReactionsByMessages(singleMessageList);
        List<Reaction> messageReactions = reactionsByMessage.getOrDefault(message.getId(), List.of());

        // Use ReactionService for reaction calculation
        MessageReactionsSummaryDto reactionsSummary = reactionService.calculateReactionsSummary(
            messageReactions, currentUser.getId(), message.getId());

        return messageMapper.toChatMessageDto(message, reactionsSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageDto> searchMessages(String query, UserDetailsImpl currentUser, Pageable pageable) {
        log.debug("Searching messages with query '{}' by user {}", query, currentUser.getId());

        Specification<Message> spec = (root, queryCB, cb) -> {
            var conversationJoin = root.join("conversation");
            var participantJoin = conversationJoin.join("participants");

            return cb.and(
                cb.equal(participantJoin.get("user").get("userId"), currentUser.getId()),
                cb.equal(participantJoin.get("isActive"), true), // Ensure participant is active
                cb.equal(root.get("deleted"), false), // Ensure message is not deleted
                cb.like(cb.lower(root.get("body")), "%" + query.toLowerCase() + "%")
            );
        };

        Page<Message> messages = messageRepository.findAll(spec, pageable);

        if (messages.isEmpty()) {
            return Page.empty(pageable);
        }

        // Use optimized bulk mapper for search results
        List<Message> messageList = messages.getContent();
        Map<Integer, List<Reaction>> reactionsByMessage = bulkDataFetcherService.getReactionsByMessages(messageList);

        List<ChatMessageDto> dtos = new ArrayList<>();
        for (Message message : messageList) {
            List<Reaction> messageReactions = reactionsByMessage.getOrDefault(message.getId(), List.of());
            MessageReactionsSummaryDto reactionsSummary = reactionService.calculateReactionsSummary(
                messageReactions, currentUser.getId(), message.getId());
            dtos.add(messageMapper.toChatMessageDto(message, reactionsSummary));
        }

        return new PageImpl<>(dtos, pageable, messages.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessagesAround(Integer conversationId, Integer aroundMessageId, UserDetailsImpl currentUser, int limit) {
        log.debug("Fetching messages around {} in conversation {} by user {}", aroundMessageId, conversationId, currentUser.getId());

        // ✅ UPDATED: Use getActiveConversationOrThrow instead of getConversationOrThrow
        Conversation conversation = entityFinderService.getActiveConversationOrThrow(conversationId);
        validateConversationMembership(conversation, currentUser);

        // ✅ UPDATED: Use getNonDeletedMessageOrThrow instead of getMessageOrThrow
        Message aroundMessage = entityFinderService.getNonDeletedMessageOrThrow(aroundMessageId);

        // Ensure limit is even for splitting, minimum 2
        int effectiveLimit = Math.max(2, (limit / 2) * 2);
        int halfLimit = effectiveLimit / 2;

        Pageable beforePageable = Pageable.ofSize(halfLimit);
        Pageable afterPageable = Pageable.ofSize(halfLimit);

        List<Message> messagesBefore = messageRepository.findMessagesBefore(
            conversationId, aroundMessage.getSentAt(), beforePageable);

        List<Message> messagesAfter = messageRepository.findMessagesAfter(
            conversationId, aroundMessage.getSentAt(), afterPageable);

        List<Message> allMessages = new ArrayList<>();
        // Add messages before in reverse order (newest first within the 'before' set)
        messagesBefore.sort((m1, m2) -> m2.getSentAt().compareTo(m1.getSentAt())); // Sort descending first
        allMessages.addAll(messagesBefore);
        allMessages.add(aroundMessage);
        // Add messages after (already sorted ascending)
        allMessages.addAll(messagesAfter);

        // Final sort to ensure overall ascending order
        allMessages.sort(java.util.Comparator.comparing(Message::getSentAt));

        // Use bulk mapper for reactions
        Map<Integer, List<Reaction>> reactionsByMessage = bulkDataFetcherService.getReactionsByMessages(allMessages);

        List<ChatMessageDto> dtos = new ArrayList<>();
        for (Message message : allMessages) {
            List<Reaction> messageReactions = reactionsByMessage.getOrDefault(message.getId(), List.of());
            MessageReactionsSummaryDto reactionsSummary = reactionService.calculateReactionsSummary(
                messageReactions, currentUser.getId(), message.getId());
            dtos.add(messageMapper.toChatMessageDto(message, reactionsSummary));
        }

        return dtos;
    }

    // ========== Utility Methods ==========

    @Override
    public void updateMessageStatus(Integer messageId, MessageStatus status, UserDetailsImpl currentUser) {
        log.debug("Updating status for message {} to {} by user {}", messageId, status, currentUser.getId());

        // ✅ UPDATED: Use getNonDeletedMessageOrThrow. If status updates can happen on deleted messages, change this.
        Message message = entityFinderService.getNonDeletedMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser); // Basic check

        switch (status) {
            case READ:
                // --- EVENT PUBLISHING INSTEAD OF DIRECT CALL ---
                log.info("Publishing MessageReadEvent for message {} read by user {}", messageId, currentUser.getId());
                MessageReadEvent event = new MessageReadEvent(this, messageId, currentUser.getId());
                eventPublisher.publishEvent(event);
                log.info("Delegated READ status update for message {} via event.", messageId);
                break;
            case SENT:
                // SENT is typically the initial state, usually no explicit update needed here.
                log.warn("Explicitly setting status to SENT for message {} is usually not required.", messageId);
                break;
            case DELIVERED:
                // This might be handled by WebSocket ACK mechanisms or similar.
                // Throwing exception as it's not implemented in the current structure.
                log.error("Updating status to DELIVERED for message {} is not supported in this implementation.", messageId);
                throw new UnsupportedOperationException("DELIVERED status update not implemented.");
            case DELETED:
                // Deletion is handled by deleteMessage method.
                log.error("Updating status to DELETED via updateMessageStatus is not the correct flow. Use deleteMessage method.");
                throw new UnsupportedOperationException("Use deleteMessage method to mark messages as deleted.");
            default:
                log.error("Unknown MessageStatus provided: {}", status);
                throw new IllegalArgumentException("Unknown MessageStatus: " + status);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canInteractWithMessage(Integer messageId, UserDetailsImpl currentUser, String action) {
        try {
            // ✅ UPDATED: Use getNonDeletedMessageOrThrow instead of getMessageOrThrow
            Message message = entityFinderService.getNonDeletedMessageOrThrow(messageId);

            // Basic check: Ensure user is part of the conversation
            validateConversationMembership(message.getConversation(), currentUser);

            boolean isOwner = message.getAuthor() != null && message.getAuthor().getUserId().equals(currentUser.getId());

            switch (action.toLowerCase()) {
                case "edit":
                    // Owner can edit if they have 'edit_own_messages', others if they have 'edit_any_message'
                    return (isOwner && hasMessagePermission(message, currentUser, "edit_own_messages")) ||
                           hasMessagePermission(message, currentUser, "edit_any_message");
                case "delete":
                    // Owner can delete if they have 'delete_own_messages', others if they have 'delete_any_message'
                    return (isOwner && hasMessagePermission(message, currentUser, "delete_own_messages")) ||
                           hasMessagePermission(message, currentUser, "delete_any_message");
                case "pin":
                    // Anyone with 'pin_messages' permission can pin/unpin
                    return hasMessagePermission(message, currentUser, "pin_messages");
                case "react":
                    // Generally, any active member can react
                    return true; // Already validated membership
                case "reply":
                     // Generally, any active member can reply
                    return true; // Already validated membership
                case "view":
                    // Generally, any active member can view
                     return true; // Already validated membership
                 case "delete_any_message": // Added explicit check for permanent delete permission
                     return hasMessagePermission(message, currentUser, "delete_any_message");
                default:
                    log.warn("Unknown interaction action '{}' requested for message {}", action, messageId);
                    return false;
            }
        } catch (ResourceNotFoundException e) {
            log.warn("Interaction check failed for non-existent message {}: {}", messageId, e.getMessage());
            return false;
        } catch (AccessDeniedException e) {
             log.warn("Interaction check failed for message {} due to membership/permission issue: {}", messageId, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during interaction check for message {}: {}", messageId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MessageStatsDto getMessageStats(Integer conversationId, UserDetailsImpl currentUser) {
        // ✅ UPDATED: Use getActiveConversationOrThrow instead of getConversationOrThrow
        Conversation conversation = entityFinderService.getActiveConversationOrThrow(conversationId);
        validateConversationMembership(conversation, currentUser);

        MessageStatsDto stats = new MessageStatsDto();
        stats.setConversationId(conversationId);
        // Use count query for better performance on total messages
        stats.setTotalMessages((int) messageRepository.countByConversationAndDeletedFalse(conversation));
        stats.setPinnedMessagesCount(messageRepository.countByConversationAndPinnedTrueAndDeletedFalse(conversation));

        // Example: Count messages sent today
        Instant startOfDay = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        Instant endOfDay = startOfDay.plus(1, java.time.temporal.ChronoUnit.DAYS);
        stats.setTodayMessageCount((int) messageRepository.countMessagesInPeriod(conversationId, startOfDay, endOfDay));

        // Get active participant count (could delegate to ParticipantService)
        stats.setActiveParticipantsCount((int) participantRepository.countActiveParticipantsByConversationId(conversationId));

        // Example: Average messages per day (requires conversation creation date)
        Instant createdAt = conversation.getCreatedAt();
        if (createdAt != null) {
            long daysSinceCreation = java.time.Duration.between(createdAt, Instant.now()).toDays();
            if (daysSinceCreation > 0) {
                stats.setAverageMessagesPerDay((double) stats.getTotalMessages() / daysSinceCreation);
            } else {
                 stats.setAverageMessagesPerDay((double) stats.getTotalMessages()); // Avoid division by zero
            }
        } else {
             stats.setAverageMessagesPerDay(0.0);
        }

        return stats;
    }

    // ========== Permission Methods ==========

    @Override
    public boolean hasMessagePermission(Integer userId, Integer conversationId, String permission) {
        Conversation conversation = entityFinderService.getActiveConversationOrThrow(conversationId);
        return permissionService.hasPermission(userId, permission, conversation);
    }

    @Override
    public boolean hasMessagePermission(UserDetailsImpl user, Integer conversationId, String permission) {
        return hasMessagePermission(user.getId(), conversationId, permission);
    }

    @Override
    public boolean hasMessagePermission(User user, Integer conversationId, String permission) {
        return hasMessagePermission(user.getUserId(), conversationId, permission);
    }

    // ========== Private Helper Methods ==========

    private Message createMessageEntity(SendMessageRequest request, Conversation conversation, User author) {
        Message message = new Message();
        message.setBody(request.getBody());
        message.setAuthor(author);
        message.setConversation(conversation);
        message.setSentAt(Instant.now());
        message.setMessageType(request.getMessageType() != null ? request.getMessageType() : MessageType.TEXT);
        message.setClientMessageId(request.getClientMessageId());

        if (request.getReplyToMessageId() != null) {
            // ✅ UPDATED: Use getNonDeletedMessageOrThrow for replyTo messages
            Message replyTo = entityFinderService.getNonDeletedMessageOrThrow(request.getReplyToMessageId());
            // Ensure reply is in the same conversation
            if (!replyTo.getConversation().getConversationId().equals(conversation.getConversationId())) {
                 throw new IllegalArgumentException("Cannot reply to a message in a different conversation.");
            }
            message.setReplyToMessage(replyTo);
        }

        if (conversation.getUniversityContext() != null) {
            message.setUniversityContext(conversation.getUniversityContext());
        }

        return message;
    }

    private void updateConversationLastMessage(Conversation conversation, Message message) {
        // Only update if this message is newer than the current last message
        if (conversation.getLastMessageSentAt() == null || message.getSentAt().isAfter(conversation.getLastMessageSentAt())) {
            conversation.setLastMessageBody(truncateMessageBody(message.getBody(), 100));
            conversation.setLastMessageSentAt(message.getSentAt());
            conversation.setLastMessageAuthor(message.getAuthor()); // Can be null for system messages
            // Increment message count only for non-system messages? Or all messages? Let's count all for now.
             conversation.setMessageCount(conversation.getMessageCount() + 1);
            conversationRepository.save(conversation);
             log.debug("Updated last message for conversation {} to message {}", conversation.getConversationId(), message.getId());
        }
         else {
             // If not updating last message details, still increment count
             conversation.setMessageCount(conversation.getMessageCount() + 1);
             conversationRepository.save(conversation);
             log.debug("Incremented message count for conversation {} without updating last message details.", conversation.getConversationId());
         }
    }

    private void updateParticipantMessageCount(Conversation conversation, User user) {
        // Only update for non-system messages
        if (user == null) {
            return;
        }
        participantRepository.findByConversationAndUser_UserIdAndIsActiveTrue(conversation, user.getUserId())
            .ifPresent(participant -> {
                participant.setMessageCount(participant.getMessageCount() + 1);
                // Also update last active time when user sends a message
                participant.setLastActiveAt(Instant.now());
                participantRepository.save(participant);
            });
    }

    private void updateLastMessageIfNeeded(Integer conversationId, Integer affectedMessageId) {
        Conversation conversation = entityFinderService.getActiveConversationOrThrow(conversationId);
        Message affectedMessage = entityFinderService.getMessageByIdEvenIfDeleted(affectedMessageId); // Check even if deleted

        // Check if the affected message was indeed the last message
        if (affectedMessage != null && conversation.getLastMessageSentAt() != null &&
            conversation.getLastMessageSentAt().equals(affectedMessage.getSentAt()) &&
            (conversation.getLastMessageAuthor() == null || affectedMessage.getAuthor() == null || // Handle system messages
             conversation.getLastMessageAuthor().getUserId().equals(affectedMessage.getAuthor().getUserId())))
        {
             log.debug("Affected message {} might be the last message. Recalculating last message for conversation {}.", affectedMessageId, conversationId);
            // Find the new latest non-deleted message
             Optional<Message> newLastMessageOpt = messageRepository.findFirstNonDeletedByConversationOrderBySentAtDesc(conversation);

            if (newLastMessageOpt.isPresent()) {
                Message newLastMessage = newLastMessageOpt.get();
                conversation.setLastMessageBody(truncateMessageBody(newLastMessage.getBody(), 100));
                conversation.setLastMessageSentAt(newLastMessage.getSentAt());
                conversation.setLastMessageAuthor(newLastMessage.getAuthor());
                log.debug("New last message for conversation {} is {}", conversationId, newLastMessage.getId());
            } else {
                // No messages left in the conversation
                conversation.setLastMessageBody(null);
                conversation.setLastMessageSentAt(null);
                conversation.setLastMessageAuthor(null);
                 log.debug("No non-deleted messages left in conversation {}. Clearing last message.", conversationId);
            }
            conversationRepository.save(conversation);
        }
         else {
             log.debug("Affected message {} was not the last message for conversation {}. No last message update needed.", affectedMessageId, conversationId);
         }
    }

    private void validateNotMuted(Conversation conversation, UserDetailsImpl user) {
        ConversationParticipant participant = getParticipantEntity(conversation, user.getId());

        if (participant.isMuted() &&
            (participant.getMutedUntil() == null || participant.getMutedUntil().isAfter(Instant.now()))) {
            throw new AccessDeniedException("You are muted in this conversation");
        }
    }

    private void validateMessageOwnershipOrPermission(Message message, UserDetailsImpl user,
                                                       String ownPermission, String anyPermission) {
        boolean isOwner = message.getAuthor() != null &&
                          message.getAuthor().getUserId().equals(user.getId());

        if (isOwner) {
             // Owner needs 'own' permission OR the 'any' permission (e.g., admin can edit/delete own messages too)
             if (!hasMessagePermission(message, user, ownPermission) && !hasMessagePermission(message, user, anyPermission)) {
                 throw new AccessDeniedException("Insufficient permissions for this action on your own message.");
             }
        } else {
             // Non-owner needs 'any' permission
             if (!hasMessagePermission(message, user, anyPermission)) {
                 throw new AccessDeniedException("Insufficient permissions for this action on another user's message.");
             }
        }
    }

    private void validateMessagePermission(Message message, UserDetailsImpl user, String permission) {
        if (!hasMessagePermission(message, user, permission)) {
            throw new AccessDeniedException("Insufficient permissions for this action: " + permission);
        }
    }

    private boolean hasMessagePermission(Message message, UserDetailsImpl user, String permission) {
        // Delegate to the main permission check method
        return hasMessagePermission(user.getId(), message.getConversation().getConversationId(), permission);
    }

    private void validateMessagePermissions(Conversation conversation, UserDetailsImpl user, String permission) {
        if (!hasMessagePermission(user.getId(), conversation.getConversationId(), permission)) {
            throw new AccessDeniedException("Insufficient permissions for this action: " + permission);
        }
    }

    private ConversationParticipant getParticipantEntity(Conversation conversation, Integer userId) {
        return participantRepository.findByConversationAndUser_UserIdAndIsActiveTrue(conversation, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Active participant not found in conversation " + conversation.getConversationId() + " for user " + userId));
    }

    private void validateConversationMembership(Conversation conversation, UserDetailsImpl user) {
        if (!participantRepository.existsByConversationAndUser_UserIdAndIsActive(conversation, user.getId(), true)) {
            throw new AccessDeniedException("You are not an active member of this conversation: " + conversation.getConversationId());
        }
    }

    private String truncateMessageBody(String body, int maxLength) {
        if (body == null) return null;
        return body.length() > maxLength ? body.substring(0, maxLength) + "..." : body;
    }
}