package com.tuniv.backend.chat.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.authorization.service.PermissionService;
import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.dto.EditMessageRequest;
import com.tuniv.backend.chat.dto.MessageReactionUpdateDto;
import com.tuniv.backend.chat.dto.MessageReactionsSummaryDto;
import com.tuniv.backend.chat.dto.MessageStatsDto;
import com.tuniv.backend.chat.dto.MessageThreadDto;
import com.tuniv.backend.chat.dto.PinnedMessageDto;
import com.tuniv.backend.chat.dto.ReactionAction;
import com.tuniv.backend.chat.dto.ReactionDto;
import com.tuniv.backend.chat.dto.ReactionRequestDto;
import com.tuniv.backend.chat.dto.ReadReceiptDto;
import com.tuniv.backend.chat.dto.SendMessageRequest;
import com.tuniv.backend.chat.dto.UnreadCountDto;
import com.tuniv.backend.chat.mapper.mapstruct.MessageMapper;
import com.tuniv.backend.chat.mapper.mapstruct.ReactionMapper;
import com.tuniv.backend.chat.mapper.mapstruct.ReadReceiptMapper;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.model.MessageStatus;
import com.tuniv.backend.chat.model.MessageType;
import com.tuniv.backend.chat.model.Reaction;
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.chat.repository.ConversationRepository;
import com.tuniv.backend.chat.repository.MessageRepository;
import com.tuniv.backend.chat.repository.ReactionRepository;
import com.tuniv.backend.chat.service.BulkDataFetcherService;
import com.tuniv.backend.chat.service.ChatRealtimeService;
import com.tuniv.backend.chat.service.EntityFinderService;
import com.tuniv.backend.chat.service.MessageService;
import com.tuniv.backend.chat.service.ReactionService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.shared.service.HtmlSanitizerService;
import com.tuniv.backend.user.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ReactionRepository reactionRepository;
    private final PermissionService permissionService;
    private final MessageMapper messageMapper;
    private final ReactionMapper reactionMapper;
    private final ReadReceiptMapper readReceiptMapper;
    private final BulkDataFetcherService bulkDataFetcherService;
    private final ChatRealtimeService chatRealtimeService;
    private final HtmlSanitizerService htmlSanitizerService;
    private final ReactionService reactionService;
    private final EntityFinderService entityFinderService;

    @Override
    @Transactional(readOnly = true)
    public boolean hasMessagePermission(Integer userId, Integer conversationId, String permission) {
        return permissionService.hasPermission(userId, permission, conversationId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasMessagePermission(UserDetailsImpl user, Integer conversationId, String permission) {
        return hasMessagePermission(user.getId(), conversationId, permission);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasMessagePermission(User user, Integer conversationId, String permission) {
        return hasMessagePermission(user.getUserId(), conversationId, permission);
    }

    @Override
    public ChatMessageDto sendMessage(Integer conversationId, SendMessageRequest request, UserDetailsImpl currentUser) {
        log.info("Sending message to conversation {} by user {}", conversationId, currentUser.getId());
        
        String sanitizedBody = htmlSanitizerService.sanitizeMessageBody(request.getBody(), true);
        request.setBody(sanitizedBody);
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        User currentUserEntity = entityFinderService.getUserOrThrow(currentUser.getId());
        
        validateConversationMembership(conversation, currentUser);
        validateMessagePermissions(conversation, currentUser, "send_messages");
        validateNotMuted(conversation, currentUser);
        
        Message message = createMessageEntity(request, conversation, currentUserEntity);
        Message savedMessage = messageRepository.save(message);
        
        updateConversationLastMessage(conversation, savedMessage);
        
        updateParticipantMessageCount(conversation, currentUserEntity);
        
        // Use bulk mapper for single message with reactions
        List<Message> singleMessageList = List.of(savedMessage);
        Map<Integer, List<Reaction>> reactionsByMessage = bulkDataFetcherService.getReactionsByMessages(singleMessageList);
        List<Reaction> messageReactions = reactionsByMessage.getOrDefault(savedMessage.getId(), List.of());
        
        // Use ReactionService for reaction calculation instead of mapper
        MessageReactionsSummaryDto reactionsSummary = reactionService.calculateReactionsSummary(
            messageReactions, currentUser.getId(), savedMessage.getId());
        
        ChatMessageDto messageDto = messageMapper.toChatMessageDto(savedMessage, reactionsSummary);
        
        chatRealtimeService.broadcastNewMessage(conversationId, messageDto);
        
        log.info("Message sent successfully with ID: {}", savedMessage.getId());
        return messageDto;
    }

    @Override
    public void createAndSendSystemMessage(Integer conversationId, String text) {
        log.info("Creating system message for conversation {}: {}", conversationId, text);
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        
        Message systemMessage = new Message();
        systemMessage.setBody(text);
        systemMessage.setConversation(conversation);
        systemMessage.setMessageType(MessageType.SYSTEM);
        systemMessage.setSentAt(Instant.now());
        systemMessage.setAuthor(null);
        
        if (conversation.getUniversityContext() != null) {
            systemMessage.setUniversityContext(conversation.getUniversityContext());
        }
        
        Message savedMessage = messageRepository.save(systemMessage);
        
        updateConversationLastMessage(conversation, savedMessage);
        
        ChatMessageDto messageDto = messageMapper.toChatMessageDto(savedMessage);
        chatRealtimeService.broadcastNewMessage(conversationId, messageDto);
        
        log.info("System message created with ID: {}", savedMessage.getId());
    }

    @Override
    public ChatMessageDto editMessage(Integer messageId, EditMessageRequest request, UserDetailsImpl currentUser) {
        log.info("Editing message {} by user {}", messageId, currentUser.getId());
        
        String sanitizedBody = htmlSanitizerService.sanitizeMessageBody(request.getBody(), true);
        request.setBody(sanitizedBody);
        
        Message message = entityFinderService.getMessageOrThrow(messageId);
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
        
        // Use ReactionService for reaction calculation instead of mapper
        MessageReactionsSummaryDto reactionsSummary = reactionService.calculateReactionsSummary(
            messageReactions, currentUser.getId(), updatedMessage.getId());
        
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
        
        Message message = entityFinderService.getMessageOrThrow(messageId);
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
        
        Message message = entityFinderService.getMessageOrThrow(messageId);
        validateMessagePermission(message, currentUser, "delete_any_message");
        
        messageRepository.delete(message);
        
        log.info("Message {} permanently deleted", messageId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getMessagesForConversation(Integer conversationId, UserDetailsImpl currentUser, Pageable pageable) {
        log.debug("Fetching messages for conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        validateConversationMembership(conversation, currentUser);
        
        Specification<Message> spec = (root, query, cb) -> 
            cb.and(
                cb.equal(root.get("conversation").get("conversationId"), conversationId),
                cb.equal(root.get("deleted"), false)
            );
        
        Page<Message> messages = messageRepository.findAll(spec, pageable);
        
        if (messages.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // Use optimized bulk mapper to avoid N+1 queries for reactions
        List<Message> messageList = messages.getContent();
        Map<Integer, List<Reaction>> reactionsByMessage = bulkDataFetcherService.getReactionsByMessages(messageList);
        
        List<ChatMessageDto> dtos = new ArrayList<>();
        for (Message message : messageList) {
            List<Reaction> messageReactions = reactionsByMessage.getOrDefault(message.getId(), List.of());
            // Use ReactionService for reaction calculation instead of mapper
            MessageReactionsSummaryDto reactionsSummary = reactionService.calculateReactionsSummary(
                messageReactions, currentUser.getId(), message.getId());
            dtos.add(messageMapper.toChatMessageDto(message, reactionsSummary));
        }
        
        return new PageImpl<>(dtos, pageable, messages.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ChatMessageDto getMessage(Integer messageId, UserDetailsImpl currentUser) {
        log.debug("Fetching message {} by user {}", messageId, currentUser.getId());
        
        Message message = entityFinderService.getMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        
        // Use bulk mapper for reactions
        List<Message> singleMessageList = List.of(message);
        Map<Integer, List<Reaction>> reactionsByMessage = bulkDataFetcherService.getReactionsByMessages(singleMessageList);
        List<Reaction> messageReactions = reactionsByMessage.getOrDefault(message.getId(), List.of());
        
        // Use ReactionService for reaction calculation instead of mapper
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
                cb.equal(participantJoin.get("active"), true),
                cb.equal(root.get("deleted"), false),
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
            // Use ReactionService for reaction calculation instead of mapper
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
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        validateConversationMembership(conversation, currentUser);
        
        Message aroundMessage = entityFinderService.getMessageOrThrow(aroundMessageId);
        
        Pageable beforePageable = Pageable.ofSize(limit / 2);
        Pageable afterPageable = Pageable.ofSize(limit / 2);
        
        List<Message> messagesBefore = messageRepository.findMessagesBefore(
            conversationId, aroundMessage.getSentAt(), beforePageable);
        
        List<Message> messagesAfter = messageRepository.findMessagesAfter(
            conversationId, aroundMessage.getSentAt(), afterPageable);
        
        List<Message> allMessages = new ArrayList<>();
        allMessages.addAll(messagesBefore);
        allMessages.add(aroundMessage);
        allMessages.addAll(messagesAfter);
        
        allMessages.sort(Comparator.comparing(Message::getSentAt));
        
        // Use bulk mapper for reactions
        Map<Integer, List<Reaction>> reactionsByMessage = bulkDataFetcherService.getReactionsByMessages(allMessages);
        
        List<ChatMessageDto> dtos = new ArrayList<>();
        for (Message message : allMessages) {
            List<Reaction> messageReactions = reactionsByMessage.getOrDefault(message.getId(), List.of());
            // Use ReactionService for reaction calculation instead of mapper
            MessageReactionsSummaryDto reactionsSummary = reactionService.calculateReactionsSummary(
                messageReactions, currentUser.getId(), message.getId());
            dtos.add(messageMapper.toChatMessageDto(message, reactionsSummary));
        }
        
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getUnreadMessages(Integer conversationId, UserDetailsImpl currentUser, Pageable pageable) {
        log.debug("Fetching unread messages for conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        ConversationParticipant participant = getParticipantEntity(conversation, currentUser.getId());
        
        if (participant.getLastReadTimestamp() == null) {
            return getMessagesForConversation(conversationId, currentUser, pageable);
        }
        
        Specification<Message> spec = (root, query, cb) -> 
            cb.and(
                cb.equal(root.get("conversation").get("conversationId"), conversationId),
                cb.equal(root.get("deleted"), false),
                cb.greaterThan(root.get("sentAt"), participant.getLastReadTimestamp())
            );
        
        Page<Message> messages = messageRepository.findAll(spec, pageable);
        
        if (messages.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // Use optimized bulk mapper
        List<Message> messageList = messages.getContent();
        Map<Integer, List<Reaction>> reactionsByMessage = bulkDataFetcherService.getReactionsByMessages(messageList);
        
        List<ChatMessageDto> dtos = new ArrayList<>();
        for (Message message : messageList) {
            List<Reaction> messageReactions = reactionsByMessage.getOrDefault(message.getId(), List.of());
            // Use ReactionService for reaction calculation instead of mapper
            MessageReactionsSummaryDto reactionsSummary = reactionService.calculateReactionsSummary(
                messageReactions, currentUser.getId(), message.getId());
            dtos.add(messageMapper.toChatMessageDto(message, reactionsSummary));
        }
        
        return new PageImpl<>(dtos, pageable, messages.getTotalElements());
    }

    @Override
    public void markMessagesAsRead(Integer conversationId, Integer lastReadMessageId, UserDetailsImpl currentUser) {
        log.debug("Marking messages as read in conversation {} up to message {} by user {}",
                 conversationId, lastReadMessageId, currentUser.getId());

        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        ConversationParticipant participant = getParticipantEntity(conversation, currentUser.getId());
        Message lastReadMessage = entityFinderService.getMessageOrThrow(lastReadMessageId);
        Instant newLastReadTimestamp = lastReadMessage.getSentAt();

        if (participant.getLastReadTimestamp() == null || newLastReadTimestamp.isAfter(participant.getLastReadTimestamp())) {
            participant.setLastReadTimestamp(newLastReadTimestamp);
            long unreadCount = messageRepository.countUnreadMessages(conversationId, newLastReadTimestamp);
            participant.setUnreadCount((int) unreadCount);
            participantRepository.save(participant);

            ReadReceiptDto readReceipt = new ReadReceiptDto(
                currentUser.getId(),
                currentUser.getUsername(),
                conversationId,
                lastReadMessageId,
                newLastReadTimestamp
            );
            chatRealtimeService.broadcastReadReceipt(conversationId, readReceipt);
            log.debug("Messages marked as read up to message {}", lastReadMessageId);
        }
    }

    @Override
    public void markAllMessagesAsRead(Integer conversationId, UserDetailsImpl currentUser) {
        log.debug("Marking all messages as read in conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        ConversationParticipant participant = getParticipantEntity(conversation, currentUser.getId());
        
        Optional<Message> latestMessage = messageRepository.findFirstByConversationOrderBySentAtDesc(conversation);
        
        if (latestMessage.isPresent()) {
            participant.setLastReadTimestamp(latestMessage.get().getSentAt());
            participant.setUnreadCount(0);
            participantRepository.save(participant);
            
            ReadReceiptDto readReceipt = new ReadReceiptDto();
            readReceipt.setUserId(currentUser.getId());
            readReceipt.setConversationId(conversationId);
            readReceipt.setLastReadTimestamp(latestMessage.get().getSentAt());
            readReceipt.setUsername(currentUser.getUsername());
            
            chatRealtimeService.broadcastReadReceipt(conversationId, readReceipt);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountDto getUnreadCount(Integer conversationId, UserDetailsImpl currentUser) {
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        ConversationParticipant participant = getParticipantEntity(conversation, currentUser.getId());
        
        UnreadCountDto unreadCount = new UnreadCountDto();
        unreadCount.setConversationId(conversationId);
        unreadCount.setUnreadCount(participant.getUnreadCount());
        unreadCount.setLastReadTimestamp(participant.getLastReadTimestamp());
        
        return unreadCount;
    }

    @Override
    public ReactionDto addOrUpdateReaction(Integer messageId, ReactionRequestDto request, UserDetailsImpl currentUser) {
        log.info("Adding/updating reaction '{}' to message {} by user {}", request.getEmoji(), messageId, currentUser.getId());
        
        Message message = entityFinderService.getMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        User currentUserEntity = entityFinderService.getUserOrThrow(currentUser.getId());
        
        Optional<Reaction> existingReactionOpt = reactionRepository.findByMessageAndUserAndEmojiAndIsRemovedFalse(
            message, currentUserEntity, request.getEmoji());
        
        Reaction reaction;
        if (existingReactionOpt.isPresent()) {
            reaction = existingReactionOpt.get();
        } else {
            reaction = new Reaction();
            reaction.setMessage(message);
            reaction.setUser(currentUserEntity);
            reaction.setEmoji(request.getEmoji());
            reaction.setSkinTone(request.getSkinTone());
            reaction.setCustomText(request.getCustomText());
            if (message.getUniversityContext() != null) {
                reaction.setUniversityContext(message.getUniversityContext());
            }
        }
        
        Reaction savedReaction = reactionRepository.save(reaction);
        ReactionDto reactionDto = reactionMapper.toReactionDto(savedReaction);
        
        MessageReactionUpdateDto reactionUpdate = new MessageReactionUpdateDto();
        reactionUpdate.setMessageId(messageId);
        reactionUpdate.setReaction(reactionDto);
        reactionUpdate.setAction(ReactionAction.ADDED);
        
        chatRealtimeService.broadcastReactionUpdate(message.getConversation().getConversationId(), reactionUpdate);
        log.info("Reaction '{}' added/updated successfully.", request.getEmoji());
        return reactionDto;
    }

    @Override
    public void removeReaction(Integer messageId, String emoji, UserDetailsImpl currentUser) {
        log.info("Removing reaction '{}' from message {} by user {}", emoji, messageId, currentUser.getId());
        
        Message message = entityFinderService.getMessageOrThrow(messageId);
        User currentUserEntity = entityFinderService.getUserOrThrow(currentUser.getId());
        
        Optional<Reaction> reactionOpt = reactionRepository.findByMessageAndUserAndEmojiAndIsRemovedFalse(
            message, currentUserEntity, emoji);
        
        if (reactionOpt.isPresent()) {
            Reaction reaction = reactionOpt.get();
        
            reaction.setRemoved(true);
            reaction.setRemovedAt(Instant.now());
            reactionRepository.save(reaction);

            ReactionDto reactionDto = reactionMapper.toReactionDto(reaction);
            
            MessageReactionUpdateDto reactionUpdate = new MessageReactionUpdateDto();
            reactionUpdate.setMessageId(messageId);
            reactionUpdate.setReaction(reactionDto);
            reactionUpdate.setAction(ReactionAction.REMOVED);
            
            chatRealtimeService.broadcastReactionUpdate(message.getConversation().getConversationId(), reactionUpdate);
            log.info("Reaction '{}' removed from message {}", emoji, messageId);
        } else {
            log.warn("Reaction '{}' not found for message {} by user {}", emoji, messageId, currentUser.getId());
        }
    }

    @Override
    public void removeReactionById(Integer reactionId, UserDetailsImpl currentUser) {
        log.info("Removing reaction by ID {} by user {}", reactionId, currentUser.getId());

        Reaction reaction = entityFinderService.getReactionOrThrow(reactionId);
        if (!reaction.getUser().getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only remove your own reactions.");
        }

        reaction.setRemoved(true);
        reaction.setRemovedAt(Instant.now());
        reactionRepository.save(reaction);

        ReactionDto reactionDto = reactionMapper.toReactionDto(reaction);

        MessageReactionUpdateDto reactionUpdate = new MessageReactionUpdateDto();
        reactionUpdate.setMessageId(reaction.getMessage().getId());
        reactionUpdate.setReaction(reactionDto);
        reactionUpdate.setAction(ReactionAction.REMOVED);

        chatRealtimeService.broadcastReactionUpdate(reaction.getMessage().getConversation().getConversationId(), reactionUpdate);
        log.info("Reaction {} removed.", reactionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReactionDto> getMessageReactions(Integer messageId, UserDetailsImpl currentUser) {
        Message message = entityFinderService.getMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        
        List<Reaction> reactions = reactionRepository.findByMessageAndIsRemovedFalse(message);
        return reactionMapper.toReactionDtoList(reactions);
    }

    @Override
    @Transactional(readOnly = true)
    public MessageReactionsSummaryDto getMessageReactionsSummary(Integer messageId, UserDetailsImpl currentUser) {
        Message message = entityFinderService.getMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        
        List<Reaction> reactions = reactionRepository.findByMessageAndIsRemovedFalse(message);
        // Use ReactionService for reaction calculation instead of mapper
        return reactionService.calculateReactionsSummary(reactions, currentUser.getId(), messageId);
    }

    @Override
    public PinnedMessageDto pinMessage(Integer messageId, UserDetailsImpl currentUser) {
        log.info("Pinning message {} by user {}", messageId, currentUser.getId());
        
        Message message = entityFinderService.getMessageOrThrow(messageId);
        User currentUserEntity = entityFinderService.getUserOrThrow(currentUser.getId());
        validateMessagePermission(message, currentUser, "pin_messages");
        
        message.setPinned(true);
        message.setPinnedAt(Instant.now());
        message.setPinnedBy(currentUserEntity);
        
        Message updatedMessage = messageRepository.save(message);
        
        PinnedMessageDto pinnedMessage = messageMapper.toPinnedMessageDto(updatedMessage);
        
        // Use bulk mapper for reactions in broadcast
        List<Message> singleMessageList = List.of(updatedMessage);
        Map<Integer, List<Reaction>> reactionsByMessage = bulkDataFetcherService.getReactionsByMessages(singleMessageList);
        List<Reaction> messageReactions = reactionsByMessage.getOrDefault(updatedMessage.getId(), List.of());
        
        // Use ReactionService for reaction calculation instead of mapper
        MessageReactionsSummaryDto reactionsSummary = reactionService.calculateReactionsSummary(
            messageReactions, currentUser.getId(), updatedMessage.getId());
        
        ChatMessageDto messageDto = messageMapper.toChatMessageDto(updatedMessage, reactionsSummary);
        
        chatRealtimeService.broadcastMessageUpdate(
            message.getConversation().getConversationId(), 
            messageDto
        );
        
        log.info("Message {} pinned successfully", messageId);
        return pinnedMessage;
    }

    @Override
    public void unpinMessage(Integer messageId, UserDetailsImpl currentUser) {
        log.info("Unpinning message {} by user {}", messageId, currentUser.getId());
        
        Message message = entityFinderService.getMessageOrThrow(messageId);
        validateMessagePermission(message, currentUser, "pin_messages");
        
        message.setPinned(false);
        messageRepository.save(message);
        
        // Use bulk mapper for reactions in broadcast
        List<Message> singleMessageList = List.of(message);
        Map<Integer, List<Reaction>> reactionsByMessage = bulkDataFetcherService.getReactionsByMessages(singleMessageList);
        List<Reaction> messageReactions = reactionsByMessage.getOrDefault(message.getId(), List.of());
        
        // Use ReactionService for reaction calculation instead of mapper
        MessageReactionsSummaryDto reactionsSummary = reactionService.calculateReactionsSummary(
            messageReactions, currentUser.getId(), message.getId());
        
        ChatMessageDto messageDto = messageMapper.toChatMessageDto(message, reactionsSummary);
        
        chatRealtimeService.broadcastMessageUpdate(
            message.getConversation().getConversationId(), 
            messageDto
        );
        
        log.info("Message {} unpinned successfully", messageId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PinnedMessageDto> getPinnedMessages(Integer conversationId, UserDetailsImpl currentUser) {
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        validateConversationMembership(conversation, currentUser);
        
        List<Message> pinnedMessages = messageRepository.findByConversationAndPinnedTrueAndDeletedFalse(conversation);
        return messageMapper.toPinnedMessageDtoList(pinnedMessages);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMessagePinned(Integer messageId, UserDetailsImpl currentUser) {
        Message message = entityFinderService.getMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        
        return message.isPinned();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getMessageReplies(Integer parentMessageId, UserDetailsImpl currentUser, Pageable pageable) {
        Message parentMessage = entityFinderService.getMessageOrThrow(parentMessageId);
        validateConversationMembership(parentMessage.getConversation(), currentUser);
        
        Specification<Message> spec = (root, query, cb) -> 
            cb.and(
                cb.equal(root.get("replyToMessage").get("id"), parentMessageId),
                cb.equal(root.get("deleted"), false)
            );
        
        Page<Message> replies = messageRepository.findAll(spec, pageable);
        
        if (replies.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // Use optimized bulk mapper for replies
        List<Message> replyList = replies.getContent();
        Map<Integer, List<Reaction>> reactionsByMessage = bulkDataFetcherService.getReactionsByMessages(replyList);
        
        List<ChatMessageDto> dtos = new ArrayList<>();
        for (Message reply : replyList) {
            List<Reaction> messageReactions = reactionsByMessage.getOrDefault(reply.getId(), List.of());
            // Use ReactionService for reaction calculation instead of mapper
            MessageReactionsSummaryDto reactionsSummary = reactionService.calculateReactionsSummary(
                messageReactions, currentUser.getId(), reply.getId());
            dtos.add(messageMapper.toChatMessageDto(reply, reactionsSummary));
        }
        
        return new PageImpl<>(dtos, pageable, replies.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public MessageThreadDto getMessageThread(Integer messageId, UserDetailsImpl currentUser) {
        Message message = entityFinderService.getMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        
        MessageThreadDto thread = new MessageThreadDto();
        
        // Use bulk mapper for parent message
        List<Message> parentMessageList = List.of(message);
        Map<Integer, List<Reaction>> parentReactions = bulkDataFetcherService.getReactionsByMessages(parentMessageList);
        List<Reaction> parentMessageReactions = parentReactions.getOrDefault(message.getId(), List.of());
        // Use ReactionService for reaction calculation instead of mapper
        MessageReactionsSummaryDto parentReactionsSummary = reactionService.calculateReactionsSummary(
            parentMessageReactions, currentUser.getId(), message.getId());
        
        thread.setParentMessage(messageMapper.toChatMessageDto(message, parentReactionsSummary));
        
        // Get replies with bulk mapping
        Page<ChatMessageDto> repliesPage = getMessageReplies(messageId, currentUser, Pageable.ofSize(10));
        thread.setReplies(repliesPage.getContent());
        thread.setReplyCount(repliesPage.getContent().size());
        
        return thread;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReadReceiptDto> getMessageReadReceipts(Integer messageId, UserDetailsImpl currentUser, Pageable pageable) {
        Message message = entityFinderService.getMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        
        List<ConversationParticipant> readers = participantRepository.findByConversationAndLastReadTimestampAfter(
            message.getConversation(), 
            message.getSentAt(),
            pageable
        );
        
        List<ReadReceiptDto> receiptDtos = readReceiptMapper.toReadReceiptDtoList(readers);
    
        return new PageImpl<>(receiptDtos, pageable, readers.size());
    }

    @Override
    public void updateMessageStatus(Integer messageId, MessageStatus status, UserDetailsImpl currentUser) {
        log.debug("Updating message {} status to {}", messageId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canInteractWithMessage(Integer messageId, UserDetailsImpl currentUser, String action) {
        try {
            Message message = entityFinderService.getMessageOrThrow(messageId);
            
            switch (action.toLowerCase()) {
                case "edit":
                    return message.getAuthor().getUserId().equals(currentUser.getId()) ||
                           hasMessagePermission(message, currentUser, "edit_any_message");
                case "delete":
                    return message.getAuthor().getUserId().equals(currentUser.getId()) ||
                           hasMessagePermission(message, currentUser, "delete_any_message");
                case "pin":
                    return hasMessagePermission(message, currentUser, "pin_messages");
                default:
                    return false;
            }
        } catch (Exception e) {
            log.warn("Interaction check failed for message {}: {}", messageId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MessageStatsDto getMessageStats(Integer conversationId, UserDetailsImpl currentUser) {
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        validateConversationMembership(conversation, currentUser);
        
        MessageStatsDto stats = new MessageStatsDto();
        stats.setConversationId(conversationId);
        stats.setTotalMessages(conversation.getMessageCount());
        stats.setPinnedMessagesCount(messageRepository.countByConversationAndPinnedTrueAndDeletedFalse(conversation));
        
        return stats;
    }

    private Message createMessageEntity(SendMessageRequest request, Conversation conversation, User author) {
        Message message = new Message();
        message.setBody(request.getBody());
        message.setAuthor(author);
        message.setConversation(conversation);
        message.setSentAt(Instant.now());
        message.setMessageType(request.getMessageType() != null ? request.getMessageType() : MessageType.TEXT);
        message.setClientMessageId(request.getClientMessageId());
        
        if (request.getReplyToMessageId() != null) {
            Message replyTo = entityFinderService.getMessageOrThrow(request.getReplyToMessageId());
            message.setReplyToMessage(replyTo);
        }
        
        if (conversation.getUniversityContext() != null) {
            message.setUniversityContext(conversation.getUniversityContext());
        }
        
        return message;
    }

    private void updateConversationLastMessage(Conversation conversation, Message message) {
        conversation.setLastMessageBody(messageMapper.truncateMessageBody(message.getBody(), 100));
        conversation.setLastMessageSentAt(message.getSentAt());
        conversation.setLastMessageAuthor(message.getAuthor());
        conversation.setMessageCount(conversation.getMessageCount() + 1);
        
        conversationRepository.save(conversation);
    }

    private void updateParticipantMessageCount(Conversation conversation, User user) {
        participantRepository.findByConversationAndUser_UserId(conversation, user.getUserId())
                .ifPresent(participant -> {
                    participant.setMessageCount(participant.getMessageCount() + 1);
                    participantRepository.save(participant);
                });
    }

    private void updateLastMessageIfNeeded(Integer conversationId, Integer affectedMessageId) {
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        
        Message affectedMessage = messageRepository.findById(affectedMessageId).orElse(null);
        if (affectedMessage == null || conversation.getLastMessageSentAt() == null ||
            !conversation.getLastMessageSentAt().equals(affectedMessage.getSentAt())) {
            return;
        }

        Optional<Message> newLastMessageOpt = messageRepository.findFirstByConversationOrderBySentAtDesc(conversation);

        if (newLastMessageOpt.isPresent()) {
            Message newLastMessage = newLastMessageOpt.get();
            conversation.setLastMessageBody(messageMapper.truncateMessageBody(newLastMessage.getBody(), 100));
            conversation.setLastMessageSentAt(newLastMessage.getSentAt());
            conversation.setLastMessageAuthor(newLastMessage.getAuthor());
        } else {
            conversation.setLastMessageBody(null);
            conversation.setLastMessageSentAt(null);
            conversation.setLastMessageAuthor(null);
        }
        conversationRepository.save(conversation);
        log.debug("Updated last message for conversation {}", conversationId);
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
            validateMessagePermission(message, user, ownPermission);
        } else {
            validateMessagePermission(message, user, anyPermission);
        }
    }

    private void validateMessagePermission(Message message, UserDetailsImpl user, String permission) {
        if (!hasMessagePermission(message, user, permission)) {
            throw new AccessDeniedException("Insufficient permissions for this action");
        }
    }

    private boolean hasMessagePermission(Message message, UserDetailsImpl user, String permission) {
        return hasMessagePermission(user.getId(), message.getConversation().getConversationId(), permission);
    }

    private void validateMessagePermissions(Conversation conversation, UserDetailsImpl user, String permission) {
        if (!hasMessagePermission(user.getId(), conversation.getConversationId(), permission)) {
            throw new AccessDeniedException("Insufficient permissions for this action");
        }
    }

    // Keep only the participant entity method since it's more complex
    private ConversationParticipant getParticipantEntity(Conversation conversation, Integer userId) {
        return participantRepository.findByConversationAndUser_UserId(conversation, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found in conversation"));
    }

    private void validateConversationMembership(Conversation conversation, UserDetailsImpl user) {
        if (!participantRepository.existsByConversationAndUser_UserIdAndIsActive(conversation, user.getId(), true)) {
            throw new AccessDeniedException("You are not a member of this conversation");
        }
    }
}