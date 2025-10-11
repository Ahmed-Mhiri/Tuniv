package com.tuniv.backend.chat.service;

import com.tuniv.backend.chat.dto.*;
import com.tuniv.backend.config.security.services.UserDetailsImpl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import com.tuniv.backend.chat.service.impl.ConversationServiceImpl.MuteDuration;

public interface ConversationService {

    // ========== Conversation Lifecycle ==========
    
    /**
     * Creates a new one-on-one direct conversation. Checks for an existing conversation first.
     */
    ConversationDetailDto startDirectConversation(StartConversationRequestDto request, UserDetailsImpl currentUser);
    
    /**
     * Creates a new group conversation. The creator becomes the first admin.
     */
    ConversationDetailDto createGroupConversation(CreateGroupRequest request, UserDetailsImpl currentUser);
    
    /**
     * Archives a conversation (soft delete).
     * Permissions: Requires 'archive_conversation' (ChatPermissions).
     */
    void archiveConversation(Integer conversationId, UserDetailsImpl currentUser);
    
    /**
     * Permanently deletes a conversation (hard delete for admins).
     * Permissions: Requires 'delete_conversation' (ChatPermissions).
     */
    void deleteConversation(Integer conversationId, UserDetailsImpl currentUser);
    
    /**
     * Restores an archived conversation.
     * Permissions: Requires 'archive_conversation' (ChatPermissions).
     */
    void restoreConversation(Integer conversationId, UserDetailsImpl currentUser);

    // ========== Data Retrieval ==========
    
    /**
     * Fetches a paginated list of all conversations for the current user.
     */
    Page<ConversationSummaryDto> getMyConversations(UserDetailsImpl currentUser, Pageable pageable);
    
    /**
     * Fetches the detailed view of a single conversation, including its participant list.
     */
    ConversationDetailDto getConversationDetails(Integer conversationId, UserDetailsImpl currentUser);
    
    /**
     * Searches conversations by title or participant names.
     */
    Page<ConversationSummaryDto> searchConversations(String query, UserDetailsImpl currentUser, Pageable pageable);
    
    /**
     * Gets archived conversations for the current user.
     */
    Page<ConversationSummaryDto> getArchivedConversations(UserDetailsImpl currentUser, Pageable pageable);

    // ========== Conversation Settings ==========
    
    /**
     * Updates a group's information (title, icon, etc.).
     * Permissions: Requires 'edit_conversation_info' (ChatPermissions).
     */
    ConversationDetailDto updateGroupInfo(Integer conversationId, UpdateGroupInfoRequest request, UserDetailsImpl currentUser);
    
    /**
     * Updates the user-specific settings for a conversation.
     */
    ParticipantDto updateMyConversationSettings(Integer conversationId, UpdateConversationSettingsRequest request, UserDetailsImpl currentUser);

    // ========== Participant Management ==========
    
    /**
     * Adds participants to a group.
     * Permissions: Requires 'add_participants' (ChatPermissions).
     */
    List<ParticipantDto> addParticipants(Integer conversationId, UpdateParticipantsRequest request, UserDetailsImpl currentUser);
    
    /**
     * Removes a participant from a group.
     * Permissions: Requires 'remove_participants' (ChatPermissions) to remove others.
     */
    void removeParticipant(Integer conversationId, Integer userIdToRemove, UserDetailsImpl currentUser);
    
    /**
     * Allows a user to leave a conversation.
     */
    void leaveConversation(Integer conversationId, UserDetailsImpl currentUser);
    
    /**
     * Updates the role of a participant.
     * Permissions: Requires 'manage_roles' (ChatPermissions).
     */
    ParticipantDto updateParticipantRole(Integer conversationId, Integer userIdToUpdate, Integer roleId, UserDetailsImpl currentUser);
    
    /**
     * Gets a list of all participants in a conversation.
     */
    List<ParticipantDto> getConversationParticipants(Integer conversationId, UserDetailsImpl currentUser);
    
    /**
     * Gets the current user's participant info for a conversation.
     */
    ParticipantDto getMyParticipantInfo(Integer conversationId, UserDetailsImpl currentUser);

    // ========== Moderation Actions ==========
    
    /**
     * Mutes a participant temporarily.
     * Permissions: Requires 'mute_participants' (ChatPermissions).
     */
    public ParticipantDto muteParticipant(Integer conversationId, Integer userIdToMute, MuteDuration duration, UserDetailsImpl currentUser);
    
    /**
     * Unmutes a participant.
     * Permissions: Requires 'mute_participants' (ChatPermissions).
     */
    ParticipantDto unmuteParticipant(Integer conversationId, Integer userIdToUnmute, UserDetailsImpl currentUser);
    
    /**
     * Bans a user from a conversation.
     * Permissions: Requires 'ban_participants' (ChatPermissions).
     */
    void banParticipant(Integer conversationId, Integer userIdToBan, String reason, UserDetailsImpl currentUser);
    
    /**
     * Unbans a previously banned user.
     * Permissions: Requires 'ban_participants' (ChatPermissions).
     */
    void unbanParticipant(Integer conversationId, Integer userIdToUnban, UserDetailsImpl currentUser);
    
    /**
     * Gets the list of banned users for a conversation.
     */
    List<BannedUserDto> getBannedParticipants(Integer conversationId, UserDetailsImpl currentUser);

    // ========== Utility Methods ==========
    
    /**
     * Checks if a direct conversation already exists between two users.
     */
    boolean directConversationExists(Integer user1Id, Integer user2Id);
    
    /**
     * Updates conversation statistics (message count, participant count, last message).
     */
    void updateConversationStats(Integer conversationId);
    
    /**
     * Validates user permissions for conversation actions.
     */
    boolean hasConversationPermission(UserDetailsImpl user, Integer conversationId, String permission);
}