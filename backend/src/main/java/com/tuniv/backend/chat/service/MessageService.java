package com.tuniv.backend.chat.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tuniv.backend.chat.dto.request.EditMessageRequest;
import com.tuniv.backend.chat.dto.request.SendMessageRequest;
import com.tuniv.backend.chat.dto.response.ChatMessageDto;
import com.tuniv.backend.chat.dto.response.MessageStatsDto;
import com.tuniv.backend.chat.model.MessageStatus;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.user.model.User;

public interface MessageService {

    // ========== Permission Methods ==========
    
    boolean hasMessagePermission(Integer userId, Integer conversationId, String permission);
    
    boolean hasMessagePermission(UserDetailsImpl user, Integer conversationId, String permission);
    
    boolean hasMessagePermission(User user, Integer conversationId, String permission);

    // ========== Core Message Actions ==========
    
    /**
     * Sends a new message to a conversation.
     * This will internally call ReactionService, ReadReceiptService, etc. if needed.
     */
    ChatMessageDto sendMessage(Integer conversationId, SendMessageRequest request, UserDetailsImpl currentUser);
    
    /**
     * Creates and sends a system message (e.g., "User joined the conversation").
     */
    void createAndSendSystemMessage(Integer conversationId, String text);
    
    /**
     * Edits an existing message.
     */
    ChatMessageDto editMessage(Integer messageId, EditMessageRequest request, UserDetailsImpl currentUser);
    
    /**
     * Deletes a message (soft delete).
     */
    void deleteMessage(Integer messageId, UserDetailsImpl currentUser);
    
    /**
     * Permanently deletes a message (hard delete for admins).
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