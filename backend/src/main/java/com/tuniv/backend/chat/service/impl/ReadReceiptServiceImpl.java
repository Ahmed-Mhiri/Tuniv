package com.tuniv.backend.chat.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.dto.ReadReceiptDto;
import com.tuniv.backend.chat.dto.UnreadCountDto;
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

    @Override
    public void markMessageAsRead(Integer messageId, UserDetailsImpl currentUser) {
        log.debug("Marking message {} as read by user {}", messageId, currentUser.getId());
        
        Message message = entityFinderService.getMessageOrThrow(messageId);
        Integer conversationId = message.getConversation().getConversationId();
        
        markMessagesAsRead(conversationId, messageId, currentUser);
    }

    @Override
    public void markMessagesAsRead(Integer conversationId, Integer lastReadMessageId, UserDetailsImpl currentUser) {
        log.debug("Marking messages as read in conversation {} up to message {} by user {}", 
                 conversationId, lastReadMessageId, currentUser.getId());
        
        // Validation
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        Message lastReadMessage = entityFinderService.getMessageOrThrow(lastReadMessageId);
        validateMessageBelongsToConversation(lastReadMessage, conversationId);
        validateConversationMembership(conversation, currentUser);
        
        // Get and update participant
        ConversationParticipant participant = entityFinderService.getActiveConversationParticipantOrThrow(
            conversationId, currentUser.getId());
        
        updateParticipantReadStatus(participant, lastReadMessage);
        
        // Broadcast read receipt
        broadcastReadReceipt(participant, lastReadMessage, conversationId);
        
        log.debug("Messages marked as read in conversation {} by user {}. Unread count: {}", 
                 conversationId, currentUser.getId(), participant.getUnreadCount());
    }

    @Override
    public void markAllMessagesAsRead(Integer conversationId, UserDetailsImpl currentUser) {
        log.debug("Marking all messages as read in conversation {} by user {}", conversationId, currentUser.getId());
        
        validateReadPermission(conversationId, currentUser);
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        Message latestMessage = messageRepository.findFirstByConversationOrderBySentAtDesc(conversation)
                .orElseThrow(() -> new ResourceNotFoundException("No messages found in conversation"));
        
        markMessagesAsRead(conversationId, latestMessage.getId(), currentUser);
        
        log.info("All messages marked as read in conversation {} by user {}", conversationId, currentUser.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountDto getUnreadCount(Integer conversationId, UserDetailsImpl currentUser) {
        log.debug("Fetching unread count for conversation {} by user {}", conversationId, currentUser.getId());
        
        validateReadPermission(conversationId, currentUser);
        
        ConversationParticipant participant = entityFinderService.getActiveConversationParticipantOrThrow(
            conversationId, currentUser.getId());
        
        return createUnreadCountDto(conversationId, participant);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getUnreadMessages(Integer conversationId, UserDetailsImpl currentUser, Pageable pageable) {
        log.debug("Fetching unread messages for conversation {} by user {}", conversationId, currentUser.getId());
        
        validateReadPermission(conversationId, currentUser);
        
        ConversationParticipant participant = entityFinderService.getActiveConversationParticipantOrThrow(
            conversationId, currentUser.getId());
        
        Instant lastReadTimestamp = participant.getLastReadTimestamp();
        
        if (lastReadTimestamp == null) {
            // If never read, return all messages
            return messageService.getMessagesForConversation(conversationId, currentUser, pageable);
        }
        
        // Get messages sent after the last read timestamp
        List<Message> unreadMessages = messageRepository.findMessagesAfter(
            conversationId, lastReadTimestamp, pageable);
        
        List<ChatMessageDto> unreadMessageDtos = chatMappingService.toChatMessageDtoList(
            unreadMessages, currentUser.getId());
        
        log.debug("Found {} unread messages for conversation {}", unreadMessageDtos.size(), conversationId);
        return new PageImpl<>(unreadMessageDtos, pageable, unreadMessages.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReadReceiptDto> getMessageReadReceipts(Integer messageId, UserDetailsImpl currentUser, Pageable pageable) {
        log.debug("Fetching read receipts for message {} by user {}", messageId, currentUser.getId());
        
        validateMessageReadPermission(messageId, currentUser);
        
        Message message = entityFinderService.getMessageOrThrow(messageId);
        
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

    // ========== Additional Utility Methods ==========

    @Override
    public void markMessagesAsRead(Integer conversationId, List<Integer> messageIds, UserDetailsImpl currentUser) {
        log.debug("Bulk marking {} messages as read in conversation {} by user {}", 
                 messageIds.size(), conversationId, currentUser.getId());
        
        if (messageIds.isEmpty()) {
            return;
        }
        
        Message latestMessage = findLatestMessageInList(messageIds);
        markMessagesAsRead(conversationId, latestMessage.getId(), currentUser);
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
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
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

    // ========== Private Helper Methods ==========

    private void updateParticipantReadStatus(ConversationParticipant participant, Message lastReadMessage) {
        Instant previousReadTime = participant.getLastReadTimestamp();
        participant.setLastReadTimestamp(lastReadMessage.getSentAt());
        
        // Calculate new unread count
        long unreadCount = calculateUnreadCount(
            participant.getConversation().getConversationId(), 
            lastReadMessage.getSentAt()
        );
        participant.setUnreadCount((int) unreadCount);
        
        participantRepository.save(participant);
    }

    private void broadcastReadReceipt(ConversationParticipant participant, Message lastReadMessage, Integer conversationId) {
        ReadReceiptDto readReceipt = chatMappingService.createBroadcastReadReceipt(participant, lastReadMessage);
        chatRealtimeService.broadcastReadReceipt(conversationId, readReceipt);
    }

    private UnreadCountDto createUnreadCountDto(Integer conversationId, ConversationParticipant participant) {
        UnreadCountDto unreadCountDto = new UnreadCountDto();
        unreadCountDto.setConversationId(conversationId);
        unreadCountDto.setUnreadCount(participant.getUnreadCount());
        unreadCountDto.setLastReadTimestamp(participant.getLastReadTimestamp());
        return unreadCountDto;
    }

    private Message findLatestMessageInList(List<Integer> messageIds) {
        return messageIds.stream()
            .map(entityFinderService::getMessageOrThrow)
            .max((m1, m2) -> m1.getSentAt().compareTo(m2.getSentAt()))
            .orElseThrow(() -> new ResourceNotFoundException("No valid messages found"));
    }

    private long calculateUnreadCount(Integer conversationId, Instant lastReadTimestamp) {
        return messageRepository.countUnreadMessages(conversationId, lastReadTimestamp);
    }

    private int calculateParticipantUnreadCount(Integer conversationId, ConversationParticipant participant) {
        if (participant.getLastReadTimestamp() != null) {
            return (int) calculateUnreadCount(conversationId, participant.getLastReadTimestamp());
        } else {
            // If never read, all messages are unread
            return (int) messageRepository.countByConversationId(conversationId);
        }
    }

    private void validateConversationMembership(Conversation conversation, UserDetailsImpl user) {
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
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        validateConversationMembership(conversation, currentUser);
    }

    private void validateMessageReadPermission(Integer messageId, UserDetailsImpl currentUser) {
        Message message = entityFinderService.getMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
    }
}