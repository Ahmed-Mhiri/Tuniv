package com.tuniv.backend.chat.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.chat.dto.ConversationDetailDto;
import com.tuniv.backend.chat.dto.ConversationSummaryDto;
import com.tuniv.backend.chat.dto.CreateGroupRequest;
import com.tuniv.backend.chat.dto.ParticipantDto;
import com.tuniv.backend.chat.dto.StartConversationRequestDto;
import com.tuniv.backend.chat.dto.UpdateGroupInfoRequest;
import com.tuniv.backend.chat.mapper.mapstruct.ConversationMapper;
import com.tuniv.backend.chat.mapper.mapstruct.MessageMapper;
import com.tuniv.backend.chat.mapper.mapstruct.ParticipantMapper;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.model.ConversationRole;
import com.tuniv.backend.chat.model.ConversationType;
import com.tuniv.backend.chat.model.DirectConversationLookup;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.repository.ConversationRepository;
import com.tuniv.backend.chat.repository.DirectConversationLookupRepository;
import com.tuniv.backend.chat.repository.MessageRepository;
import com.tuniv.backend.chat.service.BulkDataFetcherService;
import com.tuniv.backend.chat.service.ChatEventPublisher;
import com.tuniv.backend.chat.service.ConversationOnlineStatusService;
import com.tuniv.backend.chat.service.ConversationPermissionService;
import com.tuniv.backend.chat.service.ConversationRoleService;
import com.tuniv.backend.chat.service.ConversationService;
import com.tuniv.backend.chat.service.EntityFinderService;
import com.tuniv.backend.chat.service.ParticipantService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.user.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.tuniv.backend.chat.repository.ParticipantRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final DirectConversationLookupRepository directConversationLookupRepository;
    private final ConversationMapper conversationMapper;
    private final BulkDataFetcherService bulkDataFetcherService;
    private final ChatEventPublisher chatEventPublisher;
    private final EntityFinderService entityFinderService;
    private final ConversationPermissionService conversationPermissionService;
    private final ParticipantService participantService;
    private final ConversationRoleService conversationRoleService;
    private final ConversationOnlineStatusService conversationOnlineStatusService;
    private final MessageRepository messageRepository;
    private final ParticipantRepository participantRepository;
    private final ParticipantMapper participantMapper;
    private final MessageMapper messageMapper;

    @Override
    public ConversationDetailDto startDirectConversation(StartConversationRequestDto request, UserDetailsImpl currentUser) {
        log.info("Starting direct conversation between user {} and {}", currentUser.getId(), request.getTargetUserId());
        
        User currentUserEntity = entityFinderService.getUserOrThrow(currentUser.getId());
        User targetUser = entityFinderService.getUserOrThrow(request.getTargetUserId());
        
        Integer user1Id = Math.min(currentUserEntity.getUserId(), targetUser.getUserId());
        Integer user2Id = Math.max(currentUserEntity.getUserId(), targetUser.getUserId());
        
        Optional<DirectConversationLookup> existingLookup = directConversationLookupRepository
                .findByUser1IdAndUser2Id(user1Id, user2Id);
        
        if (existingLookup.isPresent()) {
            log.info("Direct conversation already exists: {}", existingLookup.get().getConversation().getConversationId());
            return getConversationDetails(existingLookup.get().getConversation().getConversationId(), currentUser);
        }
        
        validateMessagePermissions(currentUserEntity, targetUser);
        
        Conversation conversation = new Conversation();
        conversation.setTitle(generateDirectConversationTitle(currentUserEntity, targetUser));
        conversation.setConversationType(ConversationType.DIRECT);
        conversation.setUniversityContext(getUniversityContext(currentUserEntity, targetUser));
        conversation.setParticipantCount(2);
        
        Conversation savedConversation = conversationRepository.save(conversation);
        log.info("Created new direct conversation: {}", savedConversation.getConversationId());
        
        DirectConversationLookup lookup = new DirectConversationLookup(
            user1Id, user2Id, savedConversation
        );
        try {
            directConversationLookupRepository.save(lookup);
        } catch (Exception e) {
            conversationRepository.delete(savedConversation);
            existingLookup = directConversationLookupRepository.findByUser1IdAndUser2Id(user1Id, user2Id);
            if (existingLookup.isPresent()) {
                return getConversationDetails(existingLookup.get().getConversation().getConversationId(), currentUser);
            }
            throw new RuntimeException("Failed to create direct conversation due to race condition", e);
        }
        
        ConversationRole memberRole = getDefaultConversationRole();
        participantService.addParticipant(savedConversation, currentUserEntity, memberRole);
        participantService.addParticipant(savedConversation, targetUser, memberRole);
        
        chatEventPublisher.publishSystemMessageRequested(
            savedConversation.getConversationId(),
            String.format("%s started a conversation with %s", 
                currentUserEntity.getUsername(), 
                targetUser.getUsername())
        );
        
        return getConversationDetails(savedConversation.getConversationId(), currentUser);
    }

    @Override
    public ConversationDetailDto createGroupConversation(CreateGroupRequest request, UserDetailsImpl currentUser) {
        log.info("Creating group conversation '{}' by user {}", request.getTitle(), currentUser.getId());
        
        User currentUserEntity = entityFinderService.getUserOrThrow(currentUser.getId());
        
        Set<User> participants = validateAndGetParticipants(request.getParticipantIds(), currentUserEntity);
        
        Conversation conversation = new Conversation();
        conversation.setTitle(request.getTitle());
        conversation.setConversationType(ConversationType.GROUP);
        conversation.setParticipantCount(participants.size() + 1);
        
        Conversation savedConversation = conversationRepository.save(conversation);
        log.info("Created new group conversation: {}", savedConversation.getConversationId());
        
        ConversationRole adminRole = getConversationAdminRole();
        ConversationRole memberRole = getDefaultConversationRole();
        
        participantService.addParticipant(savedConversation, currentUserEntity, adminRole);
        
        participants.forEach(participant -> 
            participantService.addParticipant(savedConversation, participant, memberRole)
        );
        
        chatEventPublisher.publishSystemMessageRequested(
            savedConversation.getConversationId(),
            String.format("%s created the group '%s'", 
                currentUserEntity.getUsername(), 
                request.getTitle())
        );
        
        return getConversationDetails(savedConversation.getConversationId(), currentUser);
    }

    @Override
    public void archiveConversation(Integer conversationId, UserDetailsImpl currentUser) {
        log.info("Archiving conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        conversationPermissionService.checkPermission(currentUser, conversationId, "archive_conversation");
        
        conversation.setArchived(true);
        conversationRepository.save(conversation);
        
        log.info("Conversation {} archived successfully", conversationId);
    }

    @Override
    public void deleteConversation(Integer conversationId, UserDetailsImpl currentUser) {
        log.info("Deleting conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        conversationPermissionService.checkPermission(currentUser, conversationId, "delete_conversation");
        
        if (conversation.getConversationType() != ConversationType.DIRECT) {
            validateAdminRole(conversation, currentUser);
        }
        
        conversation.setActive(false);
        conversationRepository.save(conversation);
        
        log.info("Conversation {} deleted successfully", conversationId);
    }

    @Override
    public void restoreConversation(Integer conversationId, UserDetailsImpl currentUser) {
        log.info("Restoring conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        conversationPermissionService.checkPermission(currentUser, conversationId, "archive_conversation");
        
        conversation.setArchived(false);
        conversationRepository.save(conversation);
        
        log.info("Conversation {} restored successfully", conversationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationSummaryDto> getMyConversations(UserDetailsImpl currentUser, Pageable pageable) {
        log.debug("Fetching conversations for user {}", currentUser.getId());
        
        Page<Conversation> conversations = conversationRepository.findActiveNonArchivedConversationsByUserId(
            currentUser.getId(), pageable);
        
        if (conversations.isEmpty()) {
            return Page.empty(pageable);
        }
        
        List<ConversationSummaryDto> dtos = conversationMapper.toConversationSummaryDtoList(
            conversations.getContent(), currentUser.getId());
        
        Map<Integer, Integer> unreadCounts = bulkDataFetcherService.getUnreadCountsByConversations(
            conversations.getContent(), currentUser.getId());
        
        dtos.forEach(dto -> {
            Integer unreadCount = unreadCounts.get(dto.getConversationId());
            dto.setUnreadCount(unreadCount != null ? unreadCount : 0);
        });
        
        return new PageImpl<>(dtos, pageable, conversations.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDetailDto getConversationDetails(Integer conversationId, UserDetailsImpl currentUser) {
        log.debug("Fetching details for conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = conversationRepository.findWithParticipantsById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));
        
        conversationPermissionService.checkMembership(currentUser.getId(), conversationId);
        
        // Get online status from dedicated service
        ConversationOnlineStatusService.ConversationOnlineStatus onlineStatus = 
            conversationOnlineStatusService.getOnlineStatus(conversationId);
        
        // Build DTO with optimized data
        ConversationDetailDto dto = buildOptimizedConversationDetailDto(
            conversation, onlineStatus, currentUser);
        
        log.debug("Conversation details built with optimized data for conversation {}", conversationId);
        return dto;
    }

    private ConversationDetailDto buildOptimizedConversationDetailDto(
        Conversation conversation, 
        ConversationOnlineStatusService.ConversationOnlineStatus onlineStatus,
        UserDetailsImpl currentUser) {
    
    ConversationDetailDto dto = new ConversationDetailDto();
    
    // Map basic fields
    dto.setConversationId(conversation.getConversationId());
    dto.setTitle(conversation.getTitle());
    dto.setConversationType(conversation.getConversationType().name());
    dto.setParticipantCount(conversation.getParticipantCount());
    dto.setMessageCount(conversation.getMessageCount());
    dto.setOnlineParticipantCount(onlineStatus.getOnlineCount());
    dto.setRecentlyActiveParticipantCount(onlineStatus.getRecentlyActiveCount());
    dto.setLastActivityAt(conversation.getLastActivityAt());
    dto.setLargeGroup(conversation.isLargeGroup());
    
    // Build participant summary based on group size
    dto.setParticipantSummary(buildParticipantSummary(conversation, currentUser));
    
    // Get pinned messages (limited)
    List<Message> pinnedMessages = messageRepository.findPinnedMessages(conversation.getConversationId());
    dto.setPinnedMessages(messageMapper.toPinnedMessageDtoList(pinnedMessages));
    
    // Get current user's participant info - FIXED: using composite ID method
    ConversationParticipant currentUserParticipant = participantRepository
        .findById_UserIdAndId_ConversationId(currentUser.getId(), conversation.getConversationId())
        .orElse(null);
    if (currentUserParticipant != null) {
        dto.setCurrentUserParticipant(participantMapper.toParticipantDto(currentUserParticipant));
    }
    
    return dto;
}
    private ConversationDetailDto.ParticipantSummaryDto buildParticipantSummary(
            Conversation conversation, UserDetailsImpl currentUser) {
        
        ConversationDetailDto.ParticipantSummaryDto summary = new ConversationDetailDto.ParticipantSummaryDto();
        
        if (conversation.isLargeGroup()) {
            // For large groups, use cached data and minimal queries
            summary.setAdmins(getCachedAdmins(conversation));
            summary.setRecentlyActiveUsers(getRecentlyActiveUsers(conversation.getConversationId(), 5));
        } else {
            // For small groups, fetch fresh data
            summary.setAdmins(getFreshAdmins(conversation.getConversationId()));
            summary.setRecentlyActiveUsers(getRecentlyActiveUsers(conversation.getConversationId(), 10));
        }
        
        summary.setHasMoreParticipants(conversation.getParticipantCount() > 20);
        summary.setParticipantFetchUrl("/api/v1/conversations/" + conversation.getConversationId() + "/participants");
        
        return summary;
    }

    private List<ParticipantDto> getCachedAdmins(Conversation conversation) {
        if (conversation.getCachedAdminIds() != null && 
            conversation.getSummaryUpdatedAt() != null &&
            conversation.getSummaryUpdatedAt().isAfter(Instant.now().minus(1, ChronoUnit.HOURS))) {
            
            // Parse cached admin IDs and fetch minimal user data
            try {
                // Implementation to parse JSON and fetch user details
                return getUsersByIds(parseAdminIds(conversation.getCachedAdminIds()));
            } catch (Exception e) {
                log.warn("Failed to parse cached admin IDs for conversation {}, fetching fresh data", 
                         conversation.getConversationId());
            }
        }
        
        // Fallback to fresh data
        return getFreshAdmins(conversation.getConversationId());
    }

    private List<ParticipantDto> getFreshAdmins(Integer conversationId) {
        List<ConversationParticipant> admins = participantRepository.findActiveParticipantsByRole(
            conversationId, "CONVERSATION_ADMIN");
        return participantMapper.toParticipantDtoList(admins);
    }

    private List<ParticipantDto> getRecentlyActiveUsers(Integer conversationId, int limit) {
        // Use Redis or database to get recently active users
        List<ConversationParticipant> activeParticipants = participantRepository
            .findRecentlyActiveParticipants(conversationId, PageRequest.of(0, limit))
            .getContent();
        return participantMapper.toParticipantDtoList(activeParticipants);
    }

    private List<Integer> parseAdminIds(String cachedAdminIds) {
        // Simple JSON array parsing - implement based on your JSON library
        // This is a placeholder implementation
        try {
            // Example: "[1,2,3]" -> List.of(1,2,3)
            String clean = cachedAdminIds.replace("[", "").replace("]", "").replace("\"", "");
            return List.of(clean.split(",")).stream()
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse admin IDs", e);
        }
    }

    private List<ParticipantDto> getUsersByIds(List<Integer> userIds) {
    if (userIds == null || userIds.isEmpty()) {
        return new ArrayList<>();
    }
    
    // Fetch minimal user data for the given IDs using existing repository method
    List<ConversationParticipant> participants = participantRepository.findById_UserIdIn(userIds);
    return participantMapper.toParticipantDtoList(participants);
}

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationSummaryDto> searchConversations(String query, UserDetailsImpl currentUser, Pageable pageable) {
        log.debug("Searching conversations with query '{}' for user {}", query, currentUser.getId());
        
        Page<Conversation> conversations = conversationRepository.searchConversations(query, pageable);
        
        if (conversations.isEmpty()) {
            return Page.empty(pageable);
        }
        
        List<ConversationSummaryDto> dtos = conversationMapper.toConversationSummaryDtoList(
            conversations.getContent(), currentUser.getId());
        
        Map<Integer, Integer> unreadCounts = bulkDataFetcherService.getUnreadCountsByConversations(
            conversations.getContent(), currentUser.getId());
        
        dtos.forEach(dto -> {
            Integer unreadCount = unreadCounts.get(dto.getConversationId());
            dto.setUnreadCount(unreadCount != null ? unreadCount : 0);
        });
        
        return new PageImpl<>(dtos, pageable, conversations.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationSummaryDto> getArchivedConversations(UserDetailsImpl currentUser, Pageable pageable) {
        log.debug("Fetching archived conversations for user {}", currentUser.getId());
        
        Specification<Conversation> spec = (root, query, cb) -> {
            var participantJoin = root.join("participants");
            return cb.and(
                cb.equal(participantJoin.get("user").get("userId"), currentUser.getId()),
                cb.equal(participantJoin.get("active"), true),
                cb.equal(root.get("active"), true),
                cb.equal(root.get("archived"), true)
            );
        };
        
        Page<Conversation> conversations = conversationRepository.findAll(spec, pageable);
        
        if (conversations.isEmpty()) {
            return Page.empty(pageable);
        }
        
        List<ConversationSummaryDto> dtos = conversationMapper.toConversationSummaryDtoList(
            conversations.getContent(), currentUser.getId());
        
        Map<Integer, Integer> unreadCounts = bulkDataFetcherService.getUnreadCountsByConversations(
            conversations.getContent(), currentUser.getId());
        
        dtos.forEach(dto -> {
            Integer unreadCount = unreadCounts.get(dto.getConversationId());
            dto.setUnreadCount(unreadCount != null ? unreadCount : 0);
        });
        
        return new PageImpl<>(dtos, pageable, conversations.getTotalElements());
    }

    @Override
    public ConversationDetailDto updateGroupInfo(Integer conversationId, UpdateGroupInfoRequest request, UserDetailsImpl currentUser) {
        log.info("Updating group info for conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        conversationPermissionService.checkPermission(currentUser, conversationId, "edit_conversation_info");
        
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            conversation.setTitle(request.getTitle().trim());
        }
        
        Conversation updatedConversation = conversationRepository.save(conversation);
        
        log.info("Group info updated for conversation {}", conversationId);
        return getConversationDetails(updatedConversation.getConversationId(), currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean directConversationExists(Integer user1Id, Integer user2Id) {
        return findExistingDirectConversation(
            entityFinderService.getUserOrThrow(user1Id), 
            entityFinderService.getUserOrThrow(user2Id)
        ).isPresent();
    }

    @Override
    public void updateConversationStats(Integer conversationId) {
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        
        long activeParticipantCount = participantService.countActiveParticipants(conversation);
        conversation.setParticipantCount((int) activeParticipantCount);
        
        conversationRepository.save(conversation);
    }

    // ========== Private Helper Methods ==========

    private Optional<Conversation> findExistingDirectConversation(User user1, User user2) {
        return conversationRepository.findDirectConversationBetweenUsers(user1.getUserId(), user2.getUserId());
    }

    private ConversationRole getDefaultConversationRole() {
        return conversationRoleService.getDefaultConversationRole();
    }

    private ConversationRole getConversationAdminRole() {
        return conversationRoleService.getConversationAdminRole();
    }

    private void validateAdminRole(Conversation conversation, UserDetailsImpl user) {
        ConversationParticipant participant = participantService.getParticipantEntity(conversation, user.getId());
        if (!"CONVERSATION_ADMIN".equals(participant.getRole().getName())) {
            throw new AccessDeniedException("Admin role required for this action");
        }
    }

    private Set<User> validateAndGetParticipants(List<Integer> participantIds, User currentUser) {
        if (participantIds == null || participantIds.isEmpty()) {
            return new HashSet<>();
        }
        
        Set<User> participants = new HashSet<>(entityFinderService.getUsersByIds(participantIds));
        
        if (currentUser != null) {
            participants.removeIf(user -> user.getUserId().equals(currentUser.getUserId()));
        }
        
        if (participants.size() != participantIds.size() - (currentUser != null ? 1 : 0)) {
            throw new ResourceNotFoundException("One or more participants not found");
        }
        
        List<User> inactiveParticipants = participants.stream()
                .filter(user -> !user.isEnabled() || user.isDeleted())
                .collect(Collectors.toList());
        
        if (!inactiveParticipants.isEmpty()) {
            throw new IllegalArgumentException("One or more participants are inactive: " + 
                inactiveParticipants.stream()
                    .map(User::getUsername)
                    .collect(Collectors.joining(", ")));
        }
        
        return participants;
    }

    private void validateMessagePermissions(User sender, User receiver) {
        // Implementation depends on your business logic
    }

    private University getUniversityContext(User user1, User user2) {
        // Implementation depends on your business logic
        return null;
    }

    private String generateDirectConversationTitle(User user1, User user2) {
        return String.format("%s and %s", user1.getUsername(), user2.getUsername());
    }
}