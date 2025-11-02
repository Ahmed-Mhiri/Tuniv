package com.tuniv.backend.chat.service;

import java.util.List;

import com.tuniv.backend.chat.dto.*;
import com.tuniv.backend.chat.dto.common.ReactionDto;
import com.tuniv.backend.chat.dto.common.ReadReceiptDto;
import com.tuniv.backend.chat.dto.event.ChatNotificationDto;
import com.tuniv.backend.chat.dto.event.MessageReactionUpdateDto;
import com.tuniv.backend.chat.dto.event.SystemMessageDto;
import com.tuniv.backend.chat.dto.event.TypingIndicatorDto;
import com.tuniv.backend.chat.dto.response.BannedUserDto;
import com.tuniv.backend.chat.dto.response.ChatMessageDto;
import com.tuniv.backend.chat.dto.response.ConversationDetailDto;
import com.tuniv.backend.chat.dto.response.ParticipantDto;

public interface ChatRealtimeService {

    // ========== Message Broadcasting ==========
    
    /**
     * Broadcasts a newly created message to all participants of a conversation.
     */
    void broadcastNewMessage(Integer conversationId, ChatMessageDto messageDto);
    
    /**
     * Broadcasts that a message has been updated (edited).
     */
    void broadcastMessageUpdate(Integer conversationId, ChatMessageDto updatedMessageDto);
    
    /**
     * Broadcasts that a message has been deleted.
     */
    void broadcastMessageDeletion(Integer conversationId, Integer messageId);
    
    /**
     * Broadcasts that multiple messages have been deleted (bulk operation).
     */
    public void broadcastMessagesDeletion(Integer conversationId, List<Integer> messageIds);

    // ========== Reaction Broadcasting ==========
    
    /**
     * Broadcasts that a reaction has been added/updated/removed from a message.
     */
    void broadcastReactionUpdate(Integer conversationId, MessageReactionUpdateDto reactionUpdateDto);
    
    /**
     * Broadcasts bulk reaction updates for a message.
     */
    void broadcastReactionsUpdate(Integer conversationId, Integer messageId, List<ReactionDto> reactions);

    // ========== Typing Indicators ==========
    
    /**
     * Broadcasts a typing indicator from a user.
     */
    void broadcastTypingIndicator(Integer conversationId, TypingIndicatorDto typingIndicator);
    
    /**
     * Broadcasts that a user has stopped typing.
     */
    void broadcastStopTyping(Integer conversationId, Integer userId);

    // ========== Read Receipts ==========
    
    /**
     * Broadcasts that a user has read messages up to a certain point.
     */
    void broadcastReadReceipt(Integer conversationId, ReadReceiptDto readReceipt);
    
    /**
     * Broadcasts bulk read receipts for multiple users.
     */
    void broadcastReadReceipts(Integer conversationId, List<ReadReceiptDto> readReceipts);

    // ========== Participant Updates ==========
    
    /**
     * Broadcasts that a participant has joined the conversation.
     */
    void broadcastParticipantJoined(Integer conversationId, ParticipantDto participantDto);
    
    /**
     * Broadcasts that a participant has left the conversation.
     */
    void broadcastParticipantLeft(Integer conversationId, Integer userId, String username);
    
    /**
     * Broadcasts that a participant's role has been updated.
     */
    void broadcastParticipantRoleUpdate(Integer conversationId, ParticipantDto participantDto);
    
    /**
     * Broadcasts that a participant has been muted/unmuted.
     */
    void broadcastParticipantMuteUpdate(Integer conversationId, ParticipantDto participantDto);
    
    /**
     * Broadcasts that a participant has been banned/unbanned.
     */
    void broadcastParticipantBanUpdate(Integer conversationId, BannedUserDto bannedUserDto, boolean isBanned);

    // ========== Conversation Updates ==========
    
    /**
     * Broadcasts updates to conversation information (title, settings, etc.).
     */
    void broadcastConversationInfoUpdate(Integer conversationId, ConversationDetailDto conversationDto);
    
    /**
     * Broadcasts that conversation has been archived/restored.
     */
    void broadcastConversationArchiveUpdate(Integer conversationId, boolean isArchived);
    
    /**
     * Broadcasts that conversation has been deleted.
     */
    void broadcastConversationDeletion(Integer conversationId);

    // ========== Presence Updates ==========
    
    /**
     * Broadcasts that a user is now online/active in the conversation.
     */
    void broadcastUserPresence(Integer conversationId, Integer userId, boolean isOnline);
    
    /**
     * Broadcasts that a user's active status has changed.
     */
    void broadcastUserActiveStatus(Integer conversationId, Integer userId, boolean isActive);

    // ========== System Messages ==========
    
    /**
     * Broadcasts a system-generated message to the conversation.
     */
    void broadcastSystemMessage(Integer conversationId, SystemMessageDto systemMessage);

    // ========== Utility Methods ==========
    
    /**
     * Gets the list of currently active users in a conversation.
     */
    List<Integer> getActiveUsersInConversation(Integer conversationId);
    
    /**
     * Checks if a user is currently active/online in a conversation.
     */
    boolean isUserActiveInConversation(Integer conversationId, Integer userId);
    
    /**
     * Notifies users about new conversations they've been added to.
     */
    void notifyNewConversation(Integer conversationId, List<Integer> userIds);
    
    /**
     * Sends a direct notification to a specific user.
     */
    void sendDirectNotification(Integer userId, ChatNotificationDto notification);
}