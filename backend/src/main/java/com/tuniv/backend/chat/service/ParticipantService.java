package com.tuniv.backend.chat.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tuniv.backend.chat.dto.request.UpdateConversationSettingsRequest;
import com.tuniv.backend.chat.dto.request.UpdateParticipantsRequest;
import com.tuniv.backend.chat.dto.response.ParticipantDto;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.model.ConversationRole;
import com.tuniv.backend.chat.model.MuteDuration;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.user.model.User;

public interface ParticipantService {

    // ========== Participant Management ==========
    
    List<ParticipantDto> addParticipants(Integer conversationId, UpdateParticipantsRequest request, UserDetailsImpl currentUser);
    
    void removeParticipant(Integer conversationId, Integer userIdToRemove, UserDetailsImpl currentUser);
    
    void leaveConversation(Integer conversationId, UserDetailsImpl currentUser);
    
    ParticipantDto updateParticipantRole(Integer conversationId, Integer userIdToUpdate, Integer roleId, UserDetailsImpl currentUser);

    // ========== Participant-Specific Settings & Actions ==========

    ParticipantDto updateMyConversationSettings(Integer conversationId, UpdateConversationSettingsRequest request, UserDetailsImpl currentUser);

    ParticipantDto muteParticipant(Integer conversationId, Integer userIdToMute, MuteDuration duration, UserDetailsImpl currentUser);
    
    ParticipantDto unmuteParticipant(Integer conversationId, Integer userIdToUnmute, UserDetailsImpl currentUser);

    // ========== Data Retrieval ==========
    
    List<ParticipantDto> getConversationParticipants(Integer conversationId, UserDetailsImpl currentUser);
    
    ParticipantDto getMyParticipantInfo(Integer conversationId, UserDetailsImpl currentUser);

    // ========== Optimized Data Retrieval Methods ==========
    
    /**
     * Get conversation participants with pagination and projections for better performance
     */
    Page<ParticipantDto> getConversationParticipants(Integer conversationId, Pageable pageable);
    
    /**
     * Get recently active participants with limit for performance
     */
    List<ParticipantDto> getRecentlyActiveParticipants(Integer conversationId, int limit);
    
    /**
     * Get conversation administrators and moderators
     */
    List<ParticipantDto> getConversationAdmins(Integer conversationId);
    
    /**
     * Count active participants in a conversation (optimized version)
     */
    long countActiveParticipants(Integer conversationId);
    
    /**
     * Batch update last active timestamp for multiple users
     */
    void batchUpdateLastActive(List<Integer> userIds, Integer conversationId);

    // ========== Utility Methods ==========
    
    ConversationParticipant getParticipantEntity(Conversation conversation, Integer userId);
    
    boolean isUserParticipant(Conversation conversation, Integer userId);
    
    void validateConversationMembership(Conversation conversation, UserDetailsImpl user);
    
    ConversationParticipant addParticipant(Conversation conversation, User user, ConversationRole role);
    
    // ========== Statistics Methods ==========
    
    void updateConversationStats(Integer conversationId);
    
    long countActiveParticipants(Conversation conversation);
    
    // 🗑️ REMOVED Role Methods - Now handled by ConversationRoleService
    // ConversationRole getDefaultConversationRole();
    // ConversationRole getConversationAdminRole();
    // ConversationRole getConversationModeratorRole();
    // ConversationRole getRoleByName(String roleName);
}