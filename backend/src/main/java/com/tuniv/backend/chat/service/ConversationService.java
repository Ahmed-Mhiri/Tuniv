package com.tuniv.backend.chat.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tuniv.backend.chat.dto.BannedUserDto;
import com.tuniv.backend.chat.dto.ConversationDetailDto;
import com.tuniv.backend.chat.dto.ConversationSummaryDto;
import com.tuniv.backend.chat.dto.CreateGroupRequest;
import com.tuniv.backend.chat.dto.ParticipantDto;
import com.tuniv.backend.chat.dto.StartConversationRequestDto;
import com.tuniv.backend.chat.dto.UpdateConversationSettingsRequest;
import com.tuniv.backend.chat.dto.UpdateGroupInfoRequest;
import com.tuniv.backend.chat.dto.UpdateParticipantsRequest;
import com.tuniv.backend.chat.model.MuteDuration;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.user.model.User;

public interface ConversationService {

    // ========== Conversation Lifecycle ==========
    
    ConversationDetailDto startDirectConversation(StartConversationRequestDto request, UserDetailsImpl currentUser);
    
    ConversationDetailDto createGroupConversation(CreateGroupRequest request, UserDetailsImpl currentUser);
    
    void archiveConversation(Integer conversationId, UserDetailsImpl currentUser);
    
    void deleteConversation(Integer conversationId, UserDetailsImpl currentUser);
    
    void restoreConversation(Integer conversationId, UserDetailsImpl currentUser);

    // ========== Data Retrieval ==========
    
    Page<ConversationSummaryDto> getMyConversations(UserDetailsImpl currentUser, Pageable pageable);
    
    ConversationDetailDto getConversationDetails(Integer conversationId, UserDetailsImpl currentUser);
    
    Page<ConversationSummaryDto> searchConversations(String query, UserDetailsImpl currentUser, Pageable pageable);
    
    Page<ConversationSummaryDto> getArchivedConversations(UserDetailsImpl currentUser, Pageable pageable);

    // ========== Conversation Settings ==========
    
    ConversationDetailDto updateGroupInfo(Integer conversationId, UpdateGroupInfoRequest request, UserDetailsImpl currentUser);
    
    ParticipantDto updateMyConversationSettings(Integer conversationId, UpdateConversationSettingsRequest request, UserDetailsImpl currentUser);

    // ========== Participant Management ==========
    
    List<ParticipantDto> addParticipants(Integer conversationId, UpdateParticipantsRequest request, UserDetailsImpl currentUser);
    
    void removeParticipant(Integer conversationId, Integer userIdToRemove, UserDetailsImpl currentUser);
    
    void leaveConversation(Integer conversationId, UserDetailsImpl currentUser);
    
    ParticipantDto updateParticipantRole(Integer conversationId, Integer userIdToUpdate, Integer roleId, UserDetailsImpl currentUser);
    
    List<ParticipantDto> getConversationParticipants(Integer conversationId, UserDetailsImpl currentUser);
    
    ParticipantDto getMyParticipantInfo(Integer conversationId, UserDetailsImpl currentUser);

    // ========== Moderation Actions ==========
    
    ParticipantDto muteParticipant(Integer conversationId, Integer userIdToMute, MuteDuration duration, UserDetailsImpl currentUser);
    
    ParticipantDto unmuteParticipant(Integer conversationId, Integer userIdToUnmute, UserDetailsImpl currentUser);
    
    void banParticipant(Integer conversationId, Integer userIdToBan, String reason, UserDetailsImpl currentUser);
    
    void unbanParticipant(Integer conversationId, Integer userIdToUnban, UserDetailsImpl currentUser);
    
    List<BannedUserDto> getBannedParticipants(Integer conversationId, UserDetailsImpl currentUser);

    // ========== Utility Methods ==========
    
    boolean directConversationExists(Integer user1Id, Integer user2Id);
    
    void updateConversationStats(Integer conversationId);
    
    // ========== Permission Methods ==========
    
    boolean hasConversationPermission(Integer userId, Integer conversationId, String permission);
    
    boolean hasConversationPermission(UserDetailsImpl user, Integer conversationId, String permission);

    boolean hasConversationPermission(User user, Integer conversationId, String permission);
}