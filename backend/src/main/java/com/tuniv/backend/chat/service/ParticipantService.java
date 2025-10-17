package com.tuniv.backend.chat.service;

import java.util.List;

import com.tuniv.backend.chat.dto.ParticipantDto;
import com.tuniv.backend.chat.dto.UpdateConversationSettingsRequest;
import com.tuniv.backend.chat.dto.UpdateParticipantsRequest;
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

    // ========== Utility Methods ==========
    
    ConversationParticipant getParticipantEntity(Conversation conversation, Integer userId);
    
    boolean isUserParticipant(Conversation conversation, Integer userId);
    
    void validateConversationMembership(Conversation conversation, UserDetailsImpl user);
    
    ConversationParticipant addParticipant(Conversation conversation, User user, ConversationRole role);
    
    // ========== Statistics & Role Methods ==========
    
    void updateConversationStats(Integer conversationId);
    
    long countActiveParticipants(Conversation conversation);
    
    ConversationRole getDefaultConversationRole();
    
    ConversationRole getConversationAdminRole();
    
    ConversationRole getConversationModeratorRole();
    
    ConversationRole getRoleByName(String roleName);
}