package com.tuniv.backend.chat.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.chat.dto.common.ReadReceiptDto;
import com.tuniv.backend.chat.dto.event.UnreadCountDto;
import com.tuniv.backend.chat.dto.response.ChatMessageDto;
import com.tuniv.backend.chat.event.MessageReadEvent;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.chat.repository.MessageRepository;
import com.tuniv.backend.chat.service.ChatMappingService;
import com.tuniv.backend.chat.service.ChatRealtimeService;
import com.tuniv.backend.chat.service.EntityFinderService;
import com.tuniv.backend.chat.service.MessageService;
import com.tuniv.backend.chat.service.ReadReceiptService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReadReceiptServiceImpl implements ReadReceiptService {

    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final EntityFinderService entityFinderService;
    private final ChatRealtimeService chatRealtimeService;
    private final ChatMappingService chatMappingService;
    private final MessageService messageService;

    // ========== EVENT LISTENER METHOD ==========

    /**
     * Handles MessageReadEvent asynchronously to process read receipts.
     * This method is triggered when a MessageReadEvent is published by MessageService.
     */
    @EventListener
    @Async
    @Transactional
    public void handleMessageReadEvent(MessageReadEvent event) {
        log.debug("Received MessageReadEvent for message {} read by user {}", event.getMessageId(), event.getUserId());
        
        try {
            Integer messageId = event.getMessageId();
            Integer userId = event.getUserId();

            // Fetch necessary entities
            Message message = entityFinderService.getNonDeletedMessageOrThrow(messageId);
            Integer conversationId = message.getConversation().getConversationId();
            
            // Perform the core mark as read logic
            performMarkAsRead(conversationId, messageId, userId);
            
            log.info("Successfully processed MessageReadEvent: Marked message {} as read for user {}", messageId, userId);

        } catch (ResourceNotFoundException e) {
            log.warn("MessageReadEvent processing failed - resource not found for message {} user {}: {}", 
                    event.getMessageId(), event.getUserId(), e.getMessage());
        } catch (Exception e) {
            log.error("Error processing MessageReadEvent for message {} read by user {}: {}",
                    event.getMessageId(), event.getUserId(), e.getMessage(), e);
            // Consider adding retry logic or dead-letter queue for failed events
        }
    }

    // ========== ORIGINAL SERVICE METHODS (UPDATED TO USE SHARED LOGIC) ==========

    @Override
    public void markMessageAsRead(Integer messageId, UserDetailsImpl currentUser) {
        log.debug("Marking message {} as read by user {}", messageId, currentUser.getId());
        
        // ✅ UPDATED: Use getNonDeletedMessageOrThrow instead of getMessageOrThrow
        Message message = entityFinderService.getNonDeletedMessageOrThrow(messageId);
        Integer conversationId = message.getConversation().getConversationId();
        
        // Use the shared core logic
        performMarkAsRead(conversationId, messageId, currentUser.getId());
    }

    @Override
    public void markMessagesAsRead(Integer conversationId, Integer lastReadMessageId, UserDetailsImpl currentUser) {
        log.debug("Marking messages as read in conversation {} up to message {} by user {}", 
                 conversationId, lastReadMessageId, currentUser.getId());
        
        // Validation
        // ✅ UPDATED: Use getActiveConversationOrThrow instead of getConversationOrThrow
        Conversation conversation = entityFinderService.getActiveConversationOrThrow(conversationId);
        // ✅ UPDATED: Use getNonDeletedMessageOrThrow instead of getMessageOrThrow
        Message lastReadMessage = entityFinderService.getNonDeletedMessageOrThrow(lastReadMessageId);
        validateMessageBelongsToConversation(lastReadMessage, conversationId);
        validateConversationMembership(conversation, currentUser);
        
        // Use the shared core logic
        performMarkAsRead(conversationId, lastReadMessageId, currentUser.getId());
        
        log.debug("Messages marked as read in conversation {} by user {}", 
                 conversationId, currentUser.getId());
    }

    // ========== SHARED CORE LOGIC METHOD ==========

    /**
     * Shared core logic for marking messages as read.
     * Used by both event listener and direct service calls.
     */
    private void performMarkAsRead(Integer conversationId, Integer lastReadMessageId, Integer userId) {
        log.debug("Executing core mark-as-read logic for conversation {}, message {}, user {}", 
                 conversationId, lastReadMessageId, userId);
        
        try {
            // ✅ VERIFIED: Uses getActiveConversationParticipantOrThrow
            ConversationParticipant participant = entityFinderService.getActiveConversationParticipantOrThrow(
                conversationId, userId);
            
            // ✅ UPDATED: Use getNonDeletedMessageOrThrow instead of getMessageOrThrow
            Message lastReadMessage = entityFinderService.getNonDeletedMessageOrThrow(lastReadMessageId);
            validateMessageBelongsToConversation(lastReadMessage, conversationId);

            // Update participant read status
            updateParticipantReadStatus(participant, lastReadMessage);
            
            // Broadcast read receipt
            broadcastReadReceipt(participant, lastReadMessage, conversationId);
            
            log.debug("Core logic completed: Messages marked as read in conversation {} up to {} by user {}. Unread count: {}",
                     conversationId, lastReadMessageId, userId, participant.getUnreadCount());
                     
        } catch (Exception e) {
            log.error("Error in performMarkAsRead for conversation {}, message {}, user {}: {}", 
                     conversationId, lastReadMessageId, userId, e.getMessage(), e);
            throw e; // Re-throw to maintain existing error handling behavior
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void updateParticipantReadStatus(ConversationParticipant participant, Message lastReadMessage) {
        Instant previousReadTime = participant.getLastReadTimestamp();
        participant.setLastReadTimestamp(lastReadMessage.getSentAt());
        
        // ✅ VERIFIED: Uses repository method with explicit deleted = false filter
        long unreadCount = calculateUnreadCount(
            participant.getConversation().getConversationId(), 
            lastReadMessage.getSentAt()
        );
        participant.setUnreadCount((int) unreadCount);
        
        participantRepository.save(participant);
        
        log.debug("Updated participant read status: lastReadTimestamp={}, unreadCount={}", 
                 lastReadMessage.getSentAt(), unreadCount);
    }

    private void broadcastReadReceipt(ConversationParticipant participant, Message lastReadMessage, Integer conversationId) {
        ReadReceiptDto readReceipt = chatMappingService.createBroadcastReadReceipt(participant, lastReadMessage);
        chatRealtimeService.broadcastReadReceipt(conversationId, readReceipt);
        
        log.debug("Broadcast read receipt for conversation {}, message {}, user {}", 
                 conversationId, lastReadMessage.getId(), participant.getUser().getUserId());
    }

    private long calculateUnreadCount(Integer conversationId, Instant lastReadTimestamp) {
        // ✅ VERIFIED: Uses repository method with explicit deleted = false filter
        return messageRepository.countUnreadMessages(conversationId, lastReadTimestamp);
    }

    // ========== EXISTING METHODS (NO CHANGES NEEDED) ==========

    @Override
    public void markAllMessagesAsRead(Integer conversationId, UserDetailsImpl currentUser) {
        log.debug("Marking all messages as read in conversation {} by user {}", conversationId, currentUser.getId());
        
        validateReadPermission(conversationId, currentUser);
        
        // ✅ UPDATED: Use getActiveConversationOrThrow instead of getConversationOrThrow
        Conversation conversation = entityFinderService.getActiveConversationOrThrow(conversationId);
        // ✅ UPDATED: Use findFirstNonDeletedByConversationOrderBySentAtDesc instead of findFirstByConversationOrderBySentAtDesc
        Message latestMessage = messageRepository.findFirstNonDeletedByConversationOrderBySentAtDesc(conversation)
                .orElseThrow(() -> new ResourceNotFoundException("No messages found in conversation"));
        
        markMessagesAsRead(conversationId, latestMessage.getId(), currentUser);
        
        log.info("All messages marked as read in conversation {} by user {}", conversationId, currentUser.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReadReceiptDto> getMessageReadReceipts(Integer messageId, UserDetailsImpl currentUser, Pageable pageable) {
        log.debug("Fetching read receipts for message {} by user {}", messageId, currentUser.getId());
        
        validateMessageReadPermission(messageId, currentUser);
        
        // ✅ UPDATED: Use getNonDeletedMessageOrThrow instead of getMessageOrThrow
        Message message = entityFinderService.getNonDeletedMessageOrThrow(messageId);
        
        // Get all participants who have read this message
        List<ConversationParticipant> readers = participantRepository
            .findByConversationAndLastReadTimestampAfter(
                message.getConversation(), 
                message.getSentAt(), 
                pageable
            );
        
        List<ReadReceiptDto> readReceipts = chatMappingService.toReadReceiptDtoList(readers);
        
        log.debug("Found {} read receipts for message {}", readReceipts.size(), messageId);
        return new PageImpl<>(readReceipts, pageable, readers.size());
    }

    @Override
    public void markMessagesAsRead(Integer conversationId, List<Integer> messageIds, UserDetailsImpl currentUser) {
        log.debug("Bulk marking {} messages as read in conversation {} by user {}", 
                 messageIds.size(), conversationId, currentUser.getId());
        
        if (messageIds.isEmpty()) {
            return;
        }
        
        // ✅ UPDATED: Use getNonDeletedMessageOrThrow for each message in the list
        Message latestMessage = findLatestMessageInList(messageIds);
        markMessagesAsRead(conversationId, latestMessage.getId(), currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountDto getUnreadCount(Integer conversationId, UserDetailsImpl currentUser) {
        log.debug("Fetching unread count for conversation {} by user {}", conversationId, currentUser.getId());
        
        validateReadPermission(conversationId, currentUser);
        
        // ✅ VERIFIED: Uses getActiveConversationParticipantOrThrow
        ConversationParticipant participant = entityFinderService.getActiveConversationParticipantOrThrow(
            conversationId, currentUser.getId());
        
        return createUnreadCountDto(conversationId, participant);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getUnreadMessages(Integer conversationId, UserDetailsImpl currentUser, Pageable pageable) {
        log.debug("Fetching unread messages for conversation {} by user {}", conversationId, currentUser.getId());
        
        validateReadPermission(conversationId, currentUser);
        
        // ✅ VERIFIED: Uses getActiveConversationParticipantOrThrow
        ConversationParticipant participant = entityFinderService.getActiveConversationParticipantOrThrow(
            conversationId, currentUser.getId());
        
        Instant lastReadTimestamp = participant.getLastReadTimestamp();
        
        if (lastReadTimestamp == null) {
            // If never read, return all messages
            return messageService.getMessagesForConversation(conversationId, currentUser, pageable);
        }
        
        // ✅ VERIFIED: Uses repository method with explicit deleted = false filter
        List<Message> unreadMessages = messageRepository.findMessagesAfter(
            conversationId, lastReadTimestamp, pageable);
        
        List<ChatMessageDto> unreadMessageDtos = chatMappingService.toChatMessageDtoList(
            unreadMessages, currentUser.getId());
        
        log.debug("Found {} unread messages for conversation {}", unreadMessageDtos.size(), conversationId);
        return new PageImpl<>(unreadMessageDtos, pageable, unreadMessages.size());
    }

    @Override
    public Map<Integer, List<ReadReceiptDto>> getBulkMessageReadReceipts(List<Integer> messageIds, UserDetailsImpl currentUser) {
        log.debug("Fetching bulk read receipts for {} messages by user {}", messageIds.size(), currentUser.getId());
        
        return messageIds.stream()
            .collect(Collectors.toMap(
                messageId -> messageId,
                messageId -> getMessageReadReceipts(messageId, currentUser, Pageable.unpaged()).getContent()
            ));
    }

    @Override
    public void recalculateUnreadCounts(Integer conversationId) {
        log.debug("Recalculating unread counts for conversation {}", conversationId);
        
        // ✅ VERIFIED: Uses getActiveConversationOrThrow
        Conversation conversation = entityFinderService.getActiveConversationOrThrow(conversationId);
        // ✅ VERIFIED: Uses repository method with explicit isActive = true filter
        List<ConversationParticipant> participants = participantRepository
            .findByConversationAndIsActiveTrue(conversation);
        
        for (ConversationParticipant participant : participants) {
            int unreadCount = calculateParticipantUnreadCount(conversationId, participant);
            participant.setUnreadCount(unreadCount);
        }
        
        participantRepository.saveAll(participants);
        log.debug("Recalculated unread counts for {} participants in conversation {}", 
                 participants.size(), conversationId);
    }

    // ========== PRIVATE HELPER METHODS (NO CHANGES NEEDED) ==========

    private UnreadCountDto createUnreadCountDto(Integer conversationId, ConversationParticipant participant) {
        UnreadCountDto unreadCountDto = new UnreadCountDto();
        unreadCountDto.setConversationId(conversationId);
        unreadCountDto.setUnreadCount(participant.getUnreadCount());
        unreadCountDto.setLastReadTimestamp(participant.getLastReadTimestamp());
        return unreadCountDto;
    }

    private Message findLatestMessageInList(List<Integer> messageIds) {
        return messageIds.stream()
            // ✅ UPDATED: Use getNonDeletedMessageOrThrow for each message
            .map(entityFinderService::getNonDeletedMessageOrThrow)
            .max((m1, m2) -> m1.getSentAt().compareTo(m2.getSentAt()))
            .orElseThrow(() -> new ResourceNotFoundException("No valid messages found"));
    }

    private int calculateParticipantUnreadCount(Integer conversationId, ConversationParticipant participant) {
        if (participant.getLastReadTimestamp() != null) {
            // ✅ VERIFIED: Uses repository method with explicit deleted = false filter
            return (int) calculateUnreadCount(conversationId, participant.getLastReadTimestamp());
        } else {
            // If never read, all messages are unread
            // ✅ VERIFIED: Uses repository method with explicit deleted = false filter
            return (int) messageRepository.countByConversationId(conversationId);
        }
    }

    private void validateConversationMembership(Conversation conversation, UserDetailsImpl user) {
        // ✅ VERIFIED: Uses isUserActiveParticipant which checks active status
        if (!entityFinderService.isUserActiveParticipant(conversation.getConversationId(), user.getId())) {
            throw new ResourceNotFoundException("User is not an active member of this conversation");
        }
    }

    private void validateMessageBelongsToConversation(Message message, Integer conversationId) {
        if (!message.getConversation().getConversationId().equals(conversationId)) {
            throw new IllegalArgumentException("Message does not belong to the specified conversation");
        }
    }

    private void validateReadPermission(Integer conversationId, UserDetailsImpl currentUser) {
        // ✅ UPDATED: Use getActiveConversationOrThrow instead of getConversationOrThrow
        Conversation conversation = entityFinderService.getActiveConversationOrThrow(conversationId);
        validateConversationMembership(conversation, currentUser);
    }

    private void validateMessageReadPermission(Integer messageId, UserDetailsImpl currentUser) {
        // ✅ UPDATED: Use getNonDeletedMessageOrThrow instead of getMessageOrThrow
        Message message = entityFinderService.getNonDeletedMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
    }
}