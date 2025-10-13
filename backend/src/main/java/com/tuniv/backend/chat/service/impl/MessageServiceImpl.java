package com.tuniv.backend.chat.service.impl;

import com.tuniv.backend.authorization.service.PermissionService;
import com.tuniv.backend.chat.dto.*;
import com.tuniv.backend.chat.mapper.ChatMapper;
import com.tuniv.backend.chat.model.*;
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.chat.repository.ConversationRepository;
import com.tuniv.backend.chat.repository.MessageRepository;
import com.tuniv.backend.chat.repository.ReactionRepository;
import com.tuniv.backend.chat.service.ChatRealtimeService;
import com.tuniv.backend.chat.service.MessageService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import com.tuniv.backend.shared.service.HtmlSanitizerService;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final ChatMapper chatMapper;
    private final ChatRealtimeService chatRealtimeService;
    private final HtmlSanitizerService htmlSanitizerService;

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
        
        Conversation conversation = getConversationEntity(conversationId);
        User currentUserEntity = getUserEntity(currentUser.getId());
        
        validateConversationMembership(conversation, currentUser);
        validateMessagePermissions(conversation, currentUser, "send_messages");
        validateNotMuted(conversation, currentUser);
        
        Message message = createMessageEntity(request, conversation, currentUserEntity);
        Message savedMessage = messageRepository.save(message);
        
        updateConversationLastMessage(conversation, savedMessage);
        
        updateParticipantMessageCount(conversation, currentUserEntity);
        
        ChatMessageDto messageDto = chatMapper.toChatMessageDto(savedMessage);
        
        chatRealtimeService.broadcastNewMessage(conversationId, messageDto);
        
        log.info("Message sent successfully with ID: {}", savedMessage.getId());
        return messageDto;
    }

    @Override
    public void createAndSendSystemMessage(Integer conversationId, String text) {
        log.info("Creating system message for conversation {}: {}", conversationId, text);
        
        Conversation conversation = getConversationEntity(conversationId);
        
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
        
        ChatMessageDto messageDto = chatMapper.toChatMessageDto(savedMessage);
        chatRealtimeService.broadcastNewMessage(conversationId, messageDto);
        
        log.info("System message created with ID: {}", savedMessage.getId());
    }

    @Override
    public ChatMessageDto editMessage(Integer messageId, EditMessageRequest request, UserDetailsImpl currentUser) {
        log.info("Editing message {} by user {}", messageId, currentUser.getId());
        
        String sanitizedBody = htmlSanitizerService.sanitizeMessageBody(request.getBody(), true);
        request.setBody(sanitizedBody);
        
        Message message = getMessageEntity(messageId);
        validateMessageOwnershipOrPermission(message, currentUser, "edit_own_messages", "edit_any_message");
        
        message.setBody(request.getBody());
        message.setEdited(true);
        message.setEditedAt(Instant.now());
        message.setEditCount(message.getEditCount() + 1);
        
        Message updatedMessage = messageRepository.save(message);
        
        updateLastMessageIfNeeded(updatedMessage.getConversation().getConversationId(), messageId);
        
        ChatMessageDto messageDto = chatMapper.toChatMessageDto(updatedMessage, currentUser.getId());
        
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
        
        Message message = getMessageEntity(messageId);
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
        
        Message message = getMessageEntity(messageId);
        validateMessagePermission(message, currentUser, "delete_any_message");
        
        messageRepository.delete(message);
        
        log.info("Message {} permanently deleted", messageId);
    }

    @Override
@Transactional(readOnly = true)
public Page<ChatMessageDto> getMessagesForConversation(Integer conversationId, UserDetailsImpl currentUser, Pageable pageable) {
    log.debug("Fetching messages for conversation {} by user {}", conversationId, currentUser.getId());
    
    Conversation conversation = getConversationEntity(conversationId);
    validateConversationMembership(conversation, currentUser);
    
    Specification<Message> spec = (root, query, cb) -> 
        cb.and(
            cb.equal(root.get("conversation").get("conversationId"), conversationId),
            cb.equal(root.get("deleted"), false)
        );
    
    Page<Message> messages = messageRepository.findAll(spec, pageable);
    
    // Use optimized bulk mapper to avoid N+1 queries for reactions
    List<ChatMessageDto> dtos = chatMapper.toChatMessageDtoListOptimized(
        messages.getContent(), currentUser.getId());
    
    return new PageImpl<>(dtos, pageable, messages.getTotalElements());
}

    @Override
    @Transactional(readOnly = true)
    public ChatMessageDto getMessage(Integer messageId, UserDetailsImpl currentUser) {
        log.debug("Fetching message {} by user {}", messageId, currentUser.getId());
        
        Message message = getMessageEntity(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        
        return chatMapper.toChatMessageDto(message);
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
        return messages.map(chatMapper::toChatMessageDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessagesAround(Integer conversationId, Integer aroundMessageId, UserDetailsImpl currentUser, int limit) {
        log.debug("Fetching messages around {} in conversation {} by user {}", aroundMessageId, conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationMembership(conversation, currentUser);
        
        Message aroundMessage = getMessageEntity(aroundMessageId);
        
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
        
        return allMessages.stream()
                .map(msg -> chatMapper.toChatMessageDto(msg, currentUser.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getUnreadMessages(Integer conversationId, UserDetailsImpl currentUser, Pageable pageable) {
        log.debug("Fetching unread messages for conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
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
        return messages.map(chatMapper::toChatMessageDto);
    }

    @Override
    public void markMessagesAsRead(Integer conversationId, Integer lastReadMessageId, UserDetailsImpl currentUser) {
        log.debug("Marking messages as read in conversation {} up to message {} by user {}",
                 conversationId, lastReadMessageId, currentUser.getId());

        Conversation conversation = getConversationEntity(conversationId);
        ConversationParticipant participant = getParticipantEntity(conversation, currentUser.getId());
        Message lastReadMessage = getMessageEntity(lastReadMessageId);
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
        
        Conversation conversation = getConversationEntity(conversationId);
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
        Conversation conversation = getConversationEntity(conversationId);
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
        
        Message message = getMessageEntity(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        User currentUserEntity = getUserEntity(currentUser.getId());
        
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
        ReactionDto reactionDto = chatMapper.toReactionDto(savedReaction);
        
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
        
        Message message = getMessageEntity(messageId);
        User currentUserEntity = getUserEntity(currentUser.getId());
        
        Optional<Reaction> reactionOpt = reactionRepository.findByMessageAndUserAndEmojiAndIsRemovedFalse(
            message, currentUserEntity, emoji);
        
        if (reactionOpt.isPresent()) {
            Reaction reaction = reactionOpt.get();
        
            reaction.setRemoved(true);
            reaction.setRemovedAt(Instant.now());
            reactionRepository.save(reaction);

            ReactionDto reactionDto = chatMapper.toReactionDto(reaction);
            
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

        Reaction reaction = getReactionEntity(reactionId);
        if (!reaction.getUser().getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only remove your own reactions.");
        }

        reaction.setRemoved(true);
        reaction.setRemovedAt(Instant.now());
        reactionRepository.save(reaction);

        ReactionDto reactionDto = chatMapper.toReactionDto(reaction);

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
        Message message = getMessageEntity(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        
        return reactionRepository.findByMessageAndIsRemovedFalse(message).stream()
                .map(chatMapper::toReactionDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MessageReactionsSummaryDto getMessageReactionsSummary(Integer messageId, UserDetailsImpl currentUser) {
        Message message = getMessageEntity(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        
        List<Reaction> reactions = reactionRepository.findByMessageAndIsRemovedFalse(message);
        
        Map<String, Long> reactionCounts = reactions.stream()
                .collect(Collectors.groupingBy(
                    Reaction::getEmoji,
                    Collectors.counting()
                ));
        
        Map<String, Boolean> userReactions = reactions.stream()
                .filter(r -> r.getUser().getUserId().equals(currentUser.getId()))
                .collect(Collectors.toMap(
                    Reaction::getEmoji,
                    r -> true
                ));
        
        MessageReactionsSummaryDto summary = new MessageReactionsSummaryDto();
        summary.setMessageId(messageId);
        summary.setReactionCounts(reactionCounts);
        summary.setUserReactions(userReactions);
        summary.setTotalReactions(reactions.size());
        
        return summary;
    }

    @Override
    public PinnedMessageDto pinMessage(Integer messageId, UserDetailsImpl currentUser) {
        log.info("Pinning message {} by user {}", messageId, currentUser.getId());
        
        Message message = getMessageEntity(messageId);
        User currentUserEntity = getUserEntity(currentUser.getId());
        validateMessagePermission(message, currentUser, "pin_messages");
        
        message.setPinned(true);
        message.setPinnedAt(Instant.now());
        message.setPinnedBy(currentUserEntity);
        
        Message updatedMessage = messageRepository.save(message);
        
        PinnedMessageDto pinnedMessage = chatMapper.toPinnedMessageDto(updatedMessage);
        
        chatRealtimeService.broadcastMessageUpdate(
            message.getConversation().getConversationId(), 
            chatMapper.toChatMessageDto(updatedMessage, currentUser.getId())
        );
        
        log.info("Message {} pinned successfully", messageId);
        return pinnedMessage;
    }

    @Override
    public void unpinMessage(Integer messageId, UserDetailsImpl currentUser) {
        log.info("Unpinning message {} by user {}", messageId, currentUser.getId());
        
        Message message = getMessageEntity(messageId);
        validateMessagePermission(message, currentUser, "pin_messages");
        
        message.setPinned(false);
        messageRepository.save(message);
        
        chatRealtimeService.broadcastMessageUpdate(
            message.getConversation().getConversationId(), 
            chatMapper.toChatMessageDto(message)
        );
        
        log.info("Message {} unpinned successfully", messageId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PinnedMessageDto> getPinnedMessages(Integer conversationId, UserDetailsImpl currentUser) {
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationMembership(conversation, currentUser);
        
        return messageRepository.findByConversationAndPinnedTrueAndDeletedFalse(conversation).stream()
                .map(chatMapper::toPinnedMessageDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMessagePinned(Integer messageId, UserDetailsImpl currentUser) {
        Message message = getMessageEntity(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        
        return message.isPinned();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getMessageReplies(Integer parentMessageId, UserDetailsImpl currentUser, Pageable pageable) {
        Message parentMessage = getMessageEntity(parentMessageId);
        validateConversationMembership(parentMessage.getConversation(), currentUser);
        
        Specification<Message> spec = (root, query, cb) -> 
            cb.and(
                cb.equal(root.get("replyToMessage").get("id"), parentMessageId),
                cb.equal(root.get("deleted"), false)
            );
        
        Page<Message> replies = messageRepository.findAll(spec, pageable);
        return replies.map(chatMapper::toChatMessageDto);
    }

    @Override
    @Transactional(readOnly = true)
    public MessageThreadDto getMessageThread(Integer messageId, UserDetailsImpl currentUser) {
        Message message = getMessageEntity(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        
        MessageThreadDto thread = new MessageThreadDto();
        thread.setParentMessage(chatMapper.toChatMessageDto(message));
        
        List<ChatMessageDto> replies = getMessageReplies(messageId, currentUser, Pageable.ofSize(10))
                .getContent();
        thread.setReplies(replies);
        thread.setReplyCount(replies.size());
        
        return thread;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReadReceiptDto> getMessageReadReceipts(Integer messageId, UserDetailsImpl currentUser, Pageable pageable) {
        Message message = getMessageEntity(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        
        List<ConversationParticipant> readers = participantRepository.findByConversationAndLastReadTimestampAfter(
            message.getConversation(), 
            message.getSentAt(),
            pageable
        );
        
        List<ReadReceiptDto> receiptDtos = readers.stream()
            .map(chatMapper::toReadReceiptDto)
            .collect(Collectors.toList());
    
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
            Message message = getMessageEntity(messageId);
            
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
        Conversation conversation = getConversationEntity(conversationId);
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
            Message replyTo = getMessageEntity(request.getReplyToMessageId());
            message.setReplyToMessage(replyTo);
        }
        
        if (conversation.getUniversityContext() != null) {
            message.setUniversityContext(conversation.getUniversityContext());
        }
        
        return message;
    }

    private void updateConversationLastMessage(Conversation conversation, Message message) {
        conversation.setLastMessageBody(truncateMessageBody(message.getBody()));
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

    private String truncateMessageBody(String body) {
        if (body == null) return null;
        return body.length() > 100 ? body.substring(0, 100) + "..." : body;
    }

    private void updateLastMessageIfNeeded(Integer conversationId, Integer affectedMessageId) {
        Conversation conversation = getConversationEntity(conversationId);
        
        Message affectedMessage = messageRepository.findById(affectedMessageId).orElse(null);
        if (affectedMessage == null || conversation.getLastMessageSentAt() == null ||
            !conversation.getLastMessageSentAt().equals(affectedMessage.getSentAt())) {
            return;
        }

        Optional<Message> newLastMessageOpt = messageRepository.findFirstByConversationOrderBySentAtDesc(conversation);

        if (newLastMessageOpt.isPresent()) {
            Message newLastMessage = newLastMessageOpt.get();
            conversation.setLastMessageBody(truncateMessageBody(newLastMessage.getBody()));
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

    private User getUserEntity(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private Conversation getConversationEntity(Integer conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));
    }

    private Message getMessageEntity(Integer messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));
    }

    private Reaction getReactionEntity(Integer reactionId) {
        return reactionRepository.findById(reactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Reaction not found with id: " + reactionId));
    }

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