package com.tuniv.backend.chat.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.chat.dto.common.ConversationOnlineStatus;
import com.tuniv.backend.chat.dto.request.CreateGroupRequest;
import com.tuniv.backend.chat.dto.request.StartConversationRequestDto;
import com.tuniv.backend.chat.dto.request.UpdateGroupInfoRequest;
import com.tuniv.backend.chat.dto.response.ConversationDetailDto;
import com.tuniv.backend.chat.dto.response.ConversationSummaryDto;
import com.tuniv.backend.chat.dto.response.ParticipantDto;
import com.tuniv.backend.chat.mapper.mapstruct.ConversationMapper;
import com.tuniv.backend.chat.mapper.mapstruct.MessageMapper;
import com.tuniv.backend.chat.mapper.mapstruct.ParticipantMapper;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.model.ConversationRole;
import com.tuniv.backend.chat.model.ConversationType;
import com.tuniv.backend.chat.model.DirectConversationLookup;
import com.tuniv.backend.chat.projection.conversation.ConversationDetailProjection;
import com.tuniv.backend.chat.projection.conversation.ConversationListProjection;
import com.tuniv.backend.chat.projection.participant.ParticipantProjection;
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
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
    private final ConversationParticipantRepository participantRepository;
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
        
        // ✅ UPDATED: Use getActiveConversationOrThrow instead of getConversationOrThrow
        Conversation conversation = entityFinderService.getActiveConversationOrThrow(conversationId);
        conversationPermissionService.checkPermission(currentUser, conversationId, "archive_conversation");
        
        conversation.setArchived(true);
        conversationRepository.save(conversation);
        
        log.info("Conversation {} archived successfully", conversationId);
    }

    @Override
    public void deleteConversation(Integer conversationId, UserDetailsImpl currentUser) {
        log.info("Deleting conversation {} by user {}", conversationId, currentUser.getId());
        
        // ✅ UPDATED: Use getConversationByIdEvenIfInactive to find conversation even if already inactive
        Conversation conversation = entityFinderService.getConversationByIdEvenIfInactive(conversationId);
        
        // Check permission based on current state before deletion
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
        
        // ✅ UPDATED: Use getActiveConversationOrThrow instead of getConversationOrThrow
        Conversation conversation = entityFinderService.getActiveConversationOrThrow(conversationId);
        conversationPermissionService.checkPermission(currentUser, conversationId, "archive_conversation");
        
        conversation.setArchived(false);
        conversationRepository.save(conversation);
        
        log.info("Conversation {} restored successfully", conversationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationSummaryDto> getMyConversations(UserDetailsImpl currentUser, Pageable pageable) {
        log.debug("Fetching conversations for user {}", currentUser.getId());
        
        // 1. Call the new repository method that returns projections
        Page<ConversationListProjection> projectionPage = conversationRepository
            .findActiveNonArchivedConversationsByUserIdProjection(currentUser.getId(), pageable);
        
        // 2. Use the projection's built-in .toSummaryDto() method for mapping.
        //    This is extremely efficient. No more mappers or bulk fetchers needed.
        return projectionPage.map(ConversationListProjection::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDetailDto getConversationDetails(Integer conversationId, UserDetailsImpl currentUser) {
        log.debug("Fetching details for conversation {} by user {}", conversationId, currentUser.getId());

        // 1. Check membership first using an efficient query
        conversationPermissionService.checkMembership(currentUser.getId(), conversationId);

        // 2. Fetch the base DTO using the ConversationDetailProjection
        ConversationDetailDto dto = conversationRepository.findActiveConversationDetailById(conversationId)
            .map(ConversationDetailProjection::toDetailDto)
            .orElseThrow(() -> new ResourceNotFoundException("Active conversation not found with id: " + conversationId));

        // 3. Fetch online status (from Redis/Cache - this is fine)
        ConversationOnlineStatus onlineStatus =
            conversationOnlineStatusService.getOnlineStatus(conversationId);
        dto.setOnlineParticipantCount(onlineStatus.getOnlineCount());
        dto.setRecentlyActiveParticipantCount(onlineStatus.getRecentlyActiveCount());

        // 4. Build and set participant summary (using helpers we will refactor next)
        dto.setParticipantSummary(buildParticipantSummary(
            conversationId,
            dto.getParticipantCount(), // Pass data from projection
            dto.isLargeGroup()      // <<< FIXED: Changed getLargeGroup() to isLargeGroup()
        ));

        // 5. Fetch pinned messages (using our NEW projection)
        List<com.tuniv.backend.chat.projection.message.PinnedMessageProjection> pinnedProjections =
            messageRepository.findPinnedMessageProjections(conversationId);

        // Map projections to DTOs
        dto.setPinnedMessages(messageMapper.projectionToPinnedMessageDtoList(pinnedProjections));

        // 6. Fetch current user participant info (this query is fine as it's specific)
        ConversationParticipant currentUserParticipant = participantRepository
            .findByConversation_ConversationIdAndUser_UserIdAndIsActiveTrue(currentUser.getId(), conversationId)
            .orElse(null);

        if (currentUserParticipant != null) {
            dto.setCurrentUserParticipant(participantMapper.toParticipantDto(currentUserParticipant));
        }

        log.debug("Conversation details built with optimized data for conversation {}", conversationId);
        return dto;
    }

    // Note the new signature! We only pass the data we need from the projection.
    private ConversationDetailDto.ParticipantSummaryDto buildParticipantSummary(
            Integer conversationId, Integer participantCount, boolean isLargeGroup) {
        
        ConversationDetailDto.ParticipantSummaryDto summary = new ConversationDetailDto.ParticipantSummaryDto();
        
        if (isLargeGroup) {
            // For large groups, use minimal queries
            summary.setAdmins(getFreshAdmins(conversationId)); // This helper is already projection-based
            summary.setRecentlyActiveUsers(getRecentlyActiveUsers(conversationId, 5));
        } else {
            // For small groups, fetch fresh data
            summary.setAdmins(getFreshAdmins(conversationId));
            summary.setRecentlyActiveUsers(getRecentlyActiveUsers(conversationId, 10));
        }
        
        summary.setHasMoreParticipants(participantCount > 20); // Use the count from the projection
        summary.setParticipantFetchUrl("/api/v1/conversations/" + conversationId + "/participants");
        
        return summary;
    }

    private List<ParticipantDto> getFreshAdmins(Integer conversationId) {
        // Use the projection-based query from ParticipantRepository
        List<String> adminRoles = List.of("CONVERSATION_ADMIN", "conversation_admin", "CONVERSATION_MODERATOR");
        
        List<ParticipantProjection> projections = participantRepository
            .findActiveAdminProjectionsByConversationId(conversationId, adminRoles);
        
        // Use the mapper's projection-to-DTO list method
        return participantMapper.projectionListToParticipantDtoList(projections);
    }

    private List<ParticipantDto> getRecentlyActiveUsers(Integer conversationId, int limit) {
        // Use the projection-based query from ParticipantRepository
        List<ParticipantProjection> projections = participantRepository
            .findRecentlyActiveParticipantsSummary(conversationId, PageRequest.of(0, limit));
        
        // Use the mapper's projection-to-DTO list method
        return participantMapper.projectionListToParticipantDtoList(projections);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationSummaryDto> searchConversations(String query, UserDetailsImpl currentUser, Pageable pageable) {
        log.debug("Searching conversations with query '{}' for user {}", query, currentUser.getId());

        // <<< FIXED: Call a new projection-based repository method >>>
        // Note: You'll need to add 'searchConversationsProjection' to ConversationRepository
        // It should perform the search logic and include the unread count join similar to
        // 'findActiveNonArchivedConversationsByUserIdProjection'
        Page<ConversationListProjection> projectionPage = conversationRepository.searchConversationsProjection(
                query, currentUser.getId(), pageable);

        // <<< FIXED: Use the efficient projection mapping >>>
        return projectionPage.map(ConversationListProjection::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationSummaryDto> getArchivedConversations(UserDetailsImpl currentUser, Pageable pageable) {
        log.debug("Fetching archived conversations for user {}", currentUser.getId());

        // <<< FIXED: Call a new projection-based repository method >>>
        // Note: You'll need to add 'findArchivedConversationsByUserIdProjection' to ConversationRepository
        // It should be similar to 'findActiveNonArchivedConversationsByUserIdProjection' but filter for isArchived = true
         Page<ConversationListProjection> projectionPage = conversationRepository
                 .findArchivedConversationsByUserIdProjection(currentUser.getId(), pageable);

        // <<< FIXED: Use the efficient projection mapping >>>
        return projectionPage.map(ConversationListProjection::toSummaryDto);
    }

    @Override
    public ConversationDetailDto updateGroupInfo(Integer conversationId, UpdateGroupInfoRequest request, UserDetailsImpl currentUser) {
        log.info("Updating group info for conversation {} by user {}", conversationId, currentUser.getId());
        
        // ✅ UPDATED: Use getActiveConversationOrThrow instead of getConversationOrThrow
        Conversation conversation = entityFinderService.getActiveConversationOrThrow(conversationId);
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
        // ✅ VERIFIED: Uses proper user fetching
        return findExistingDirectConversation(
            entityFinderService.getUserOrThrow(user1Id), 
            entityFinderService.getUserOrThrow(user2Id)
        ).isPresent();
    }

    @Override
    public void updateConversationStats(Integer conversationId) {
        // ✅ UPDATED: Use getActiveConversationOrThrow instead of getConversationOrThrow
        Conversation conversation = entityFinderService.getActiveConversationOrThrow(conversationId);
        
        long activeParticipantCount = participantService.countActiveParticipants(conversation);
        conversation.setParticipantCount((int) activeParticipantCount);
        
        conversationRepository.save(conversation);
    }

    // ========== Private Helper Methods ==========

    private Optional<Conversation> findExistingDirectConversation(User user1, User user2) {
        // Note: This uses the deprecated repository method. Consider updating to use active-aware version
        // if you have one in your repository
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