package com.tuniv.backend.chat.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.dto.PinnedMessageDto;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.model.Reaction;
import com.tuniv.backend.chat.repository.MessageRepository;
import com.tuniv.backend.chat.service.BulkDataFetcherService;
import com.tuniv.backend.chat.service.ChatMappingService;
import com.tuniv.backend.chat.service.ChatRealtimeService;
import com.tuniv.backend.chat.service.EntityFinderService;
import com.tuniv.backend.chat.service.PinnedMessageService;
import com.tuniv.backend.chat.service.ReactionService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.user.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PinnedMessageServiceImpl implements PinnedMessageService {

    private final MessageRepository messageRepository;
    private final EntityFinderService entityFinderService;
    private final ChatRealtimeService chatRealtimeService;
    private final BulkDataFetcherService bulkDataFetcherService;
    private final ReactionService reactionService;
    private final ChatMappingService chatMappingService; // NEW: Added mapping service

    @Override
    public PinnedMessageDto pinMessage(Integer messageId, UserDetailsImpl currentUser) {
        log.info("Pinning message {} by user {}", messageId, currentUser.getId());
        
        Message message = entityFinderService.getMessageOrThrow(messageId);
        User currentUserEntity = entityFinderService.getUserOrThrow(currentUser.getId());
        
        validateConversationMembership(message.getConversation(), currentUser);
        validatePinPermissions(message, currentUser);
        validateMessageNotAlreadyPinned(message);
        validateMaxPinnedMessagesLimit(message.getConversation());
        
        message.setPinned(true);
        message.setPinnedAt(Instant.now());
        message.setPinnedBy(currentUserEntity);
        
        Message updatedMessage = messageRepository.save(message);
        
        // UPDATED: Use mapping service
        PinnedMessageDto pinnedMessage = chatMappingService.toPinnedMessageDto(updatedMessage);
        
        // Broadcast the message update to all conversation participants
        broadcastPinnedMessageUpdate(updatedMessage, currentUser);
        
        log.info("Message {} pinned successfully by user {}", messageId, currentUser.getId());
        return pinnedMessage;
    }

    @Override
    public void unpinMessage(Integer messageId, UserDetailsImpl currentUser) {
        log.info("Unpinning message {} by user {}", messageId, currentUser.getId());
        
        Message message = entityFinderService.getMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        validatePinPermissions(message, currentUser);
        validateMessageIsPinned(message);
        
        message.setPinned(false);
        message.setPinnedAt(null);
        message.setPinnedBy(null);
        
        Message updatedMessage = messageRepository.save(message);
        
        // Broadcast the message update to all conversation participants
        broadcastPinnedMessageUpdate(updatedMessage, currentUser);
        
        log.info("Message {} unpinned successfully by user {}", messageId, currentUser.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PinnedMessageDto> getPinnedMessages(Integer conversationId, UserDetailsImpl currentUser) {
        log.debug("Fetching pinned messages for conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        validateConversationMembership(conversation, currentUser);
        
        List<Message> pinnedMessages = messageRepository.findByConversationAndPinnedTrueAndDeletedFalseOrderByPinnedAtDesc(conversation);
        
        if (pinnedMessages.isEmpty()) {
            log.debug("No pinned messages found for conversation {}", conversationId);
            return List.of();
        }
        
        // UPDATED: Use mapping service for bulk conversion
        List<PinnedMessageDto> pinnedMessageDtos = chatMappingService.toPinnedMessageDtoList(
            pinnedMessages, currentUser.getId());
        
        log.debug("Found {} pinned messages for conversation {}", pinnedMessageDtos.size(), conversationId);
        return pinnedMessageDtos;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMessagePinned(Integer messageId, UserDetailsImpl currentUser) {
        log.debug("Checking if message {} is pinned by user {}", messageId, currentUser.getId());
        
        Message message = entityFinderService.getMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        
        boolean isPinned = message.isPinned();
        log.debug("Message {} pinned status: {}", messageId, isPinned);
        return isPinned;
    }

    /**
     * Gets the count of pinned messages in a conversation
     */
    @Transactional(readOnly = true)
    public long getPinnedMessagesCount(Integer conversationId, UserDetailsImpl currentUser) {
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        validateConversationMembership(conversation, currentUser);
        
        Long count = messageRepository.countByConversationAndPinnedTrueAndDeletedFalse(conversation);
        log.debug("Pinned messages count for conversation {}: {}", conversationId, count);
        return count;
    }

    /**
     * Unpins all messages in a conversation (useful for conversation cleanup)
     */
    public void unpinAllMessages(Integer conversationId, UserDetailsImpl currentUser) {
        log.info("Unpinning all messages in conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        validateConversationMembership(conversation, currentUser);
        validatePinPermissions(conversation, currentUser, "pin_messages");
        
        List<Message> pinnedMessages = messageRepository.findByConversationAndPinnedTrueAndDeletedFalse(conversation);
        
        if (pinnedMessages.isEmpty()) {
            log.debug("No pinned messages to unpin in conversation {}", conversationId);
            return;
        }
        
        // Unpin all messages
        pinnedMessages.forEach(message -> {
            message.setPinned(false);
            message.setPinnedAt(null);
            message.setPinnedBy(null);
        });
        
        messageRepository.saveAll(pinnedMessages);
        
        // Broadcast updates for all unpinned messages
        pinnedMessages.forEach(message -> 
            broadcastPinnedMessageUpdate(message, currentUser)
        );
        
        log.info("Unpinned {} messages in conversation {}", pinnedMessages.size(), conversationId);
    }

    /**
     * Gets recently pinned messages (within the last specified days)
     */
    @Transactional(readOnly = true)
    public List<PinnedMessageDto> getRecentlyPinnedMessages(Integer conversationId, int days, UserDetailsImpl currentUser) {
        log.debug("Fetching messages pinned within last {} days for conversation {} by user {}", 
                 days, conversationId, currentUser.getId());
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        validateConversationMembership(conversation, currentUser);
        
        Instant since = Instant.now().minusSeconds(days * 24 * 60 * 60L);
        List<Message> recentlyPinnedMessages = messageRepository
            .findByConversationAndPinnedTrueAndPinnedAtAfterAndDeletedFalseOrderByPinnedAtDesc(conversation, since);
        
        // UPDATED: Use mapping service for bulk conversion
        List<PinnedMessageDto> pinnedMessageDtos = chatMappingService.toPinnedMessageDtoList(
            recentlyPinnedMessages, currentUser.getId());
        
        log.debug("Found {} recently pinned messages for conversation {}", pinnedMessageDtos.size(), conversationId);
        return pinnedMessageDtos;
    }

    // ========== Private Helper Methods ==========
    
    private void validatePinPermissions(Message message, UserDetailsImpl currentUser) {
        validatePinPermissions(message.getConversation(), currentUser, "pin_messages");
    }
    
    private void validatePinPermissions(Conversation conversation, UserDetailsImpl currentUser, String permission) {
        try {
            entityFinderService.getConversationParticipantOrThrow(conversation.getConversationId(), currentUser.getId());
        } catch (Exception e) {
            throw new AccessDeniedException("Insufficient permissions to pin/unpin messages");
        }
    }
    
    private void validateMessageNotAlreadyPinned(Message message) {
        if (message.isPinned()) {
            throw new IllegalStateException("Message is already pinned");
        }
    }
    
    private void validateMessageIsPinned(Message message) {
        if (!message.isPinned()) {
            throw new IllegalStateException("Message is not pinned");
        }
    }
    
    private void validateMaxPinnedMessagesLimit(Conversation conversation) {
        long currentPinnedCount = messageRepository.countByConversationAndPinnedTrueAndDeletedFalse(conversation);
        int maxPinnedMessages = 50; // Configurable limit
        
        if (currentPinnedCount >= maxPinnedMessages) {
            throw new IllegalStateException(
                String.format("Cannot pin more than %d messages in a conversation", maxPinnedMessages)
            );
        }
    }
    
    private void validateConversationMembership(Conversation conversation, UserDetailsImpl user) {
        try {
            entityFinderService.getConversationParticipantOrThrow(conversation.getConversationId(), user.getId());
        } catch (Exception e) {
            throw new AccessDeniedException("You are not a member of this conversation");
        }
    }
    
    private void broadcastPinnedMessageUpdate(Message message, UserDetailsImpl currentUser) {
        try {
            // UPDATED: Use mapping service for conversion
            ChatMessageDto messageDto = chatMappingService.toChatMessageDto(
                message, chatMappingService.createReactionsSummary(message, currentUser.getId()));
            
            chatRealtimeService.broadcastMessageUpdate(
                message.getConversation().getConversationId(), 
                messageDto
            );
            
            log.debug("Broadcast pinned message update for message {}", message.getId());
        } catch (Exception e) {
            log.error("Failed to broadcast pinned message update for message {}: {}", 
                     message.getId(), e.getMessage(), e);
        }
    }
}