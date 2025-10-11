package com.tuniv.backend.chat.service;

import com.tuniv.backend.chat.dto.*;
import com.tuniv.backend.config.security.services.UserDetailsImpl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import com.tuniv.backend.chat.model.MessageStatus;

public interface MessageService {

    // ========== Core Message Actions ==========
    
    /**
     * Sends a new message to a conversation.
     * Permissions: Requires 'send_messages' (ChatPermissions).
     */
    ChatMessageDto sendMessage(Integer conversationId, SendMessageRequest request, UserDetailsImpl currentUser);
    
    /**
     * Creates and sends a system message (e.g., "User joined the conversation").
     */
    void createAndSendSystemMessage(Integer conversationId, String text);
    
    /**
     * Edits an existing message.
     * Permissions: Requires 'edit_own_messages' for self, or 'edit_any_message' for moderators.
     */
    ChatMessageDto editMessage(Integer messageId, EditMessageRequest request, UserDetailsImpl currentUser);
    
    /**
     * Deletes a message (soft delete).
     * Permissions: Requires 'delete_own_messages' for self, or 'delete_any_message' for moderators.
     */
    void deleteMessage(Integer messageId, UserDetailsImpl currentUser);
    
    /**
     * Permanently deletes a message (hard delete for admins).
     * Permissions: Requires 'delete_any_message' (ChatPermissions).
     */
    void permanentlyDeleteMessage(Integer messageId, UserDetailsImpl currentUser);

    // ========== Message Retrieval ==========
    
    /**
     * Fetches a paginated history of messages for a conversation.
     */
    Page<ChatMessageDto> getMessagesForConversation(Integer conversationId, UserDetailsImpl currentUser, Pageable pageable);
    
    /**
     * Gets a single message by ID with full details.
     */
    ChatMessageDto getMessage(Integer messageId, UserDetailsImpl currentUser);
    
    /**
     * Searches for messages within a user's conversations.
     */
    Page<ChatMessageDto> searchMessages(String query, UserDetailsImpl currentUser, Pageable pageable);
    
    /**
     * Gets messages around a specific message (for infinite scroll).
     */
    List<ChatMessageDto> getMessagesAround(Integer conversationId, Integer aroundMessageId, UserDetailsImpl currentUser, int limit);
    
    /**
     * Gets unread messages for a conversation.
     */
    Page<ChatMessageDto> getUnreadMessages(Integer conversationId, UserDetailsImpl currentUser, Pageable pageable);

    // ========== Message Interactions ==========
    
    /**
     * Marks messages as read for the current user.
     */
    void markMessagesAsRead(Integer conversationId, List<Integer> messageIds, UserDetailsImpl currentUser);
    
    /**
     * Marks all messages in a conversation as read.
     */
    void markAllMessagesAsRead(Integer conversationId, UserDetailsImpl currentUser);
    
    /**
     * Gets the unread message count for a conversation.
     */
    UnreadCountDto getUnreadCount(Integer conversationId, UserDetailsImpl currentUser);

    // ========== Reactions ==========
    
    /**
     * Adds or updates a user's reaction to a message.
     */
    ReactionDto addOrUpdateReaction(Integer messageId, ReactionRequestDto request, UserDetailsImpl currentUser);
    
    /**
     * Removes a reaction from a message.
     */
    void removeReaction(Integer messageId, String emoji, UserDetailsImpl currentUser);
    
    /**
     * Removes a specific reaction by ID.
     */
    void removeReactionById(Integer reactionId, UserDetailsImpl currentUser);
    
    /**
     * Gets all reactions for a specific message.
     */
    List<ReactionDto> getMessageReactions(Integer messageId, UserDetailsImpl currentUser);
    
    /**
     * Gets reaction summary for a message (counts by emoji).
     */
    MessageReactionsSummaryDto getMessageReactionsSummary(Integer messageId, UserDetailsImpl currentUser);

    // ========== Pinning ==========
    
    /**
     * Pins a message to the conversation.
     * Permissions: Requires 'pin_messages' (ChatPermissions).
     */
    PinnedMessageDto pinMessage(Integer messageId, UserDetailsImpl currentUser);
    
    /**
     * Unpins a message.
     * Permissions: Requires 'pin_messages' (ChatPermissions).
     */
    void unpinMessage(Integer messageId, UserDetailsImpl currentUser);
    
    /**
     * Gets all pinned messages for a conversation.
     */
    List<PinnedMessageDto> getPinnedMessages(Integer conversationId, UserDetailsImpl currentUser);
    
    /**
     * Checks if a message is pinned.
     */
    boolean isMessagePinned(Integer messageId, UserDetailsImpl currentUser);

    // ========== Threads & Replies ==========
    
    /**
     * Gets replies to a specific message (thread view).
     */
    Page<ChatMessageDto> getMessageReplies(Integer parentMessageId, UserDetailsImpl currentUser, Pageable pageable);
    
    /**
     * Gets the reply thread for a message.
     */
    MessageThreadDto getMessageThread(Integer messageId, UserDetailsImpl currentUser);

    // ========== Utility Methods ==========
    
    /**
     * Updates message delivery status.
     */
    void updateMessageStatus(Integer messageId, MessageStatus status, UserDetailsImpl currentUser);
    
    /**
     * Validates if user can interact with a message.
     */
    boolean canInteractWithMessage(Integer messageId, UserDetailsImpl currentUser, String action);
    
    /**
     * Gets message statistics for a conversation.
     */
    MessageStatsDto getMessageStats(Integer conversationId, UserDetailsImpl currentUser);
}