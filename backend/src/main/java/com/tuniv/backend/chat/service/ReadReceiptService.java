package com.tuniv.backend.chat.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tuniv.backend.chat.dto.common.ReadReceiptDto;
import com.tuniv.backend.chat.dto.event.UnreadCountDto;
import com.tuniv.backend.chat.dto.response.ChatMessageDto;
import com.tuniv.backend.config.security.services.UserDetailsImpl;

public interface ReadReceiptService {

    /**
     * Marks messages as read for the current user up to a specific message ID.
     * This method implementation will find the conversationId internally.
     */
    void markMessageAsRead(Integer messageId, UserDetailsImpl currentUser);

    /**
     * Marks messages as read for the current user up to a specific message ID in a conversation.
     */
    void markMessagesAsRead(Integer conversationId, Integer lastReadMessageId, UserDetailsImpl currentUser);
    
    /**
     * Marks all messages in a conversation as read for the current user.
     */
    void markAllMessagesAsRead(Integer conversationId, UserDetailsImpl currentUser);
    
    /**
     * Gets the unread message count for a conversation for the current user.
     */
    UnreadCountDto getUnreadCount(Integer conversationId, UserDetailsImpl currentUser);
    
    /**
     * Gets unread messages for a conversation for the current user.
     */
    Page<ChatMessageDto> getUnreadMessages(Integer conversationId, UserDetailsImpl currentUser, Pageable pageable);
    
    /**
     * Gets a paginated list of read receipts (who read the message) for a specific message.
     */
    Page<ReadReceiptDto> getMessageReadReceipts(Integer messageId, UserDetailsImpl currentUser, Pageable pageable);

    // ========== Additional Utility Methods ==========

    /**
     * Bulk marks multiple messages as read in a conversation for the current user.
     * Uses the latest message in the list as the last read message.
     */
    void markMessagesAsRead(Integer conversationId, List<Integer> messageIds, UserDetailsImpl currentUser);

    /**
     * Gets read receipts for multiple messages in bulk.
     * Returns a map where key is message ID and value is list of read receipts for that message.
     */
    Map<Integer, List<ReadReceiptDto>> getBulkMessageReadReceipts(List<Integer> messageIds, UserDetailsImpl currentUser);

    /**
     * Recalculates unread counts for all participants in a conversation.
     * Useful when messages are deleted or when system needs to sync read status.
     */
    void recalculateUnreadCounts(Integer conversationId);
}