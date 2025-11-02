package com.tuniv.backend.chat.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.tuniv.backend.chat.dto.request.CreateGroupRequest;
import com.tuniv.backend.chat.dto.request.StartConversationRequestDto;
import com.tuniv.backend.chat.dto.request.UpdateGroupInfoRequest;
import com.tuniv.backend.chat.dto.response.ConversationDetailDto;
import com.tuniv.backend.chat.dto.response.ConversationSummaryDto;
import com.tuniv.backend.config.security.services.UserDetailsImpl;

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

    // ========== Utility Methods ==========
    
    boolean directConversationExists(Integer user1Id, Integer user2Id);
    
    void updateConversationStats(Integer conversationId);
}