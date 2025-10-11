package com.tuniv.backend.chat.service.impl;

import com.tuniv.backend.authorization.model.Permission;
import com.tuniv.backend.authorization.service.PermissionService;
import com.tuniv.backend.chat.dto.*;
import com.tuniv.backend.chat.mapper.ChatMapper;
import com.tuniv.backend.chat.model.*;
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.chat.repository.ConversationRepository;
import com.tuniv.backend.chat.repository.ConversationRoleRepository;
import com.tuniv.backend.chat.service.ConversationService;
import com.tuniv.backend.chat.service.MessageService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ConversationRoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final ChatMapper chatMapper;
    private final MessageService messageService;

    // ========== Conversation Lifecycle ==========

    @Override
    public ConversationDetailDto startDirectConversation(StartConversationRequestDto request, UserDetailsImpl currentUser) {
        log.info("Starting direct conversation between user {} and {}", currentUser.getId(), request.getTargetUserId());
        
        User currentUserEntity = getUserEntity(currentUser.getId());
        User targetUser = getUserEntity(request.getTargetUserId());
        
        // Check if direct conversation already exists
        Optional<Conversation> existingConversation = findExistingDirectConversation(currentUserEntity, targetUser);
        if (existingConversation.isPresent()) {
            log.info("Direct conversation already exists: {}", existingConversation.get().getConversationId());
            return chatMapper.toConversationDetailDto(existingConversation.get(), currentUserEntity);
        }
        
        // Validate that users can message each other
        validateMessagePermissions(currentUserEntity, targetUser);
        
        // Create new conversation
        Conversation conversation = new Conversation();
        conversation.setTitle(generateDirectConversationTitle(currentUserEntity, targetUser));
        conversation.setConversationType(ConversationType.DIRECT);
        conversation.setUniversityContext(getUniversityContext(currentUserEntity, targetUser));
        conversation.setParticipantCount(2);
        
        Conversation savedConversation = conversationRepository.save(conversation);
        log.info("Created new direct conversation: {}", savedConversation.getConversationId());
        
        // Add participants with appropriate roles
        ConversationRole memberRole = getDefaultConversationRole();
        addParticipant(savedConversation, currentUserEntity, memberRole);
        addParticipant(savedConversation, targetUser, memberRole);
        
        // Create welcome system message
        messageService.createAndSendSystemMessage(
            savedConversation.getConversationId(),
            String.format("%s started a conversation with %s", 
                currentUserEntity.getUsername(), 
                targetUser.getUsername())
        );
        
        return chatMapper.toConversationDetailDto(savedConversation, currentUserEntity);
    }

    @Override
    public ConversationDetailDto createGroupConversation(CreateGroupRequest request, UserDetailsImpl currentUser) {
        log.info("Creating group conversation '{}' by user {}", request.getTitle(), currentUser.getId());
        
        User currentUserEntity = getUserEntity(currentUser.getId());
        
        // Validate participants
        Set<User> participants = validateAndGetParticipants(request.getParticipantIds(), currentUserEntity);
        
        // Create conversation
        Conversation conversation = new Conversation();
        conversation.setTitle(request.getTitle());
        conversation.setConversationType(ConversationType.GROUP);
        // University context will be handled by a separate service later
        // conversation.setUniversityContext(getUniversityContext(currentUserEntity, null));

        conversation.setParticipantCount(participants.size() + 1); // +1 for creator
        
        Conversation savedConversation = conversationRepository.save(conversation);
        log.info("Created new group conversation: {}", savedConversation.getConversationId());
        
        // Add participants with roles
        ConversationRole adminRole = getConversationAdminRole();
        ConversationRole memberRole = getDefaultConversationRole();
        
        // Creator becomes admin
        addParticipant(savedConversation, currentUserEntity, adminRole);
        
        // Other participants become members
        participants.forEach(participant -> 
            addParticipant(savedConversation, participant, memberRole)
        );
        
        // Create welcome system message
        messageService.createAndSendSystemMessage(
            savedConversation.getConversationId(),
            String.format("%s created the group '%s'", 
                currentUserEntity.getUsername(), 
                request.getTitle())
        );
        
        return chatMapper.toConversationDetailDto(savedConversation, currentUserEntity);
    }

    @Override
    public void archiveConversation(Integer conversationId, UserDetailsImpl currentUser) {
        log.info("Archiving conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationPermission(conversation, currentUser, "archive_conversation");
        
        conversation.setArchived(true);
        conversationRepository.save(conversation);
        
        log.info("Conversation {} archived successfully", conversationId);
    }

    @Override
    public void deleteConversation(Integer conversationId, UserDetailsImpl currentUser) {
        log.info("Deleting conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationPermission(conversation, currentUser, "delete_conversation");
        
        // For group conversations, require admin role
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
        
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationPermission(conversation, currentUser, "archive_conversation");
        
        conversation.setArchived(false);
        conversationRepository.save(conversation);
        
        log.info("Conversation {} restored successfully", conversationId);
    }

    // ========== Data Retrieval ==========

    @Override
@Transactional(readOnly = true)
public Page<ConversationSummaryDto> getMyConversations(UserDetailsImpl currentUser, Pageable pageable) {
    log.debug("Fetching conversations for user {}", currentUser.getId());
    
    Specification<Conversation> spec = (root, query, cb) -> {
        var participantJoin = root.join("participants");
        return cb.and(
            cb.equal(participantJoin.get("user").get("userId"), currentUser.getId()),
            cb.equal(participantJoin.get("active"), true),
            cb.equal(root.get("active"), true),
            cb.equal(root.get("archived"), false)
        );
    };
    
    Page<Conversation> conversations = conversationRepository.findAll(spec, pageable);

// Use the optimized method to avoid N+1 queries
List<ConversationSummaryDto> dtos = chatMapper.toConversationSummaryDtoListOptimized(
    conversations.getContent(), currentUser.getId()
);
return new org.springframework.data.domain.PageImpl<>(dtos, pageable, conversations.getTotalElements());
}

    @Override
    @Transactional(readOnly = true)
    public ConversationDetailDto getConversationDetails(Integer conversationId, UserDetailsImpl currentUser) {
        log.debug("Fetching details for conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
        .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));
        validateConversationMembership(conversation, currentUser);
        
        User currentUserEntity = getUserEntity(currentUser.getId());
        return chatMapper.toConversationDetailDto(conversation, currentUserEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationSummaryDto> searchConversations(String query, UserDetailsImpl currentUser, Pageable pageable) {
        log.debug("Searching conversations with query '{}' for user {}", query, currentUser.getId());
        
        Specification<Conversation> spec = (root, queryCB, cb) -> {
            var participantJoin = root.join("participants");
            
            var searchPredicate = cb.or(
                cb.like(cb.lower(root.get("title")), "%" + query.toLowerCase() + "%")
                // Add participant name search if needed
            );
            
            return cb.and(
                cb.equal(participantJoin.get("user").get("userId"), currentUser.getId()),
                cb.equal(participantJoin.get("active"), true),
                cb.equal(root.get("active"), true),
                cb.equal(root.get("archived"), false),
                searchPredicate
            );
        };
        
        Page<Conversation> conversations = conversationRepository.findAll(spec, pageable);
        return conversations.map(conv -> chatMapper.toConversationSummaryDto(conv, currentUser.getId()));
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
        return conversations.map(conv -> chatMapper.toConversationSummaryDto(conv, currentUser.getId()));
    }

    // ========== Conversation Settings ==========

    @Override
    public ConversationDetailDto updateGroupInfo(Integer conversationId, UpdateGroupInfoRequest request, UserDetailsImpl currentUser) {
        log.info("Updating group info for conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationPermission(conversation, currentUser, "edit_conversation_info");
        
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            conversation.setTitle(request.getTitle().trim());
        }
        
        // Add other fields as needed (icon, description, etc.)
        
        Conversation updatedConversation = conversationRepository.save(conversation);
        User currentUserEntity = getUserEntity(currentUser.getId());
        
        log.info("Group info updated for conversation {}", conversationId);
        return chatMapper.toConversationDetailDto(updatedConversation, currentUserEntity);
    }

    @Override
    public ParticipantDto updateMyConversationSettings(Integer conversationId, UpdateConversationSettingsRequest request, UserDetailsImpl currentUser) {
        log.info("Updating conversation settings for user {} in conversation {}", currentUser.getId(), conversationId);
        
        Conversation conversation = getConversationEntity(conversationId);
        ConversationParticipant participant = getParticipantEntity(conversation, currentUser.getId());
        
        if (request.getNickname() != null) {
            participant.setNickname(request.getNickname().trim());
        }
        
        if (request.getNotificationsEnabled() != null) {
            participant.setNotificationsEnabled(request.getNotificationsEnabled());
        }
        
        if (request.getIsMuted() != null) {
            participant.setMuted(request.getIsMuted());
            if (request.getIsMuted()) {
                participant.setMutedUntil(Instant.now().plus(24, ChronoUnit.HOURS)); // Default 24h mute
            } else {
                participant.setMutedUntil(null);
            }
        }
        
        ConversationParticipant updatedParticipant = participantRepository.save(participant);
        
        log.info("Conversation settings updated for user {} in conversation {}", currentUser.getId(), conversationId);
        return chatMapper.toParticipantDto(updatedParticipant);
    }

    // ========== Participant Management ==========

    @Override
    public List<ParticipantDto> addParticipants(Integer conversationId, UpdateParticipantsRequest request, UserDetailsImpl currentUser) {
        log.info("Adding participants to conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationPermission(conversation, currentUser, "add_participants");
        
        Set<User> newParticipants = validateAndGetParticipants(request.getUserIds(), null);
        ConversationRole memberRole = getDefaultConversationRole();
        
        List<ParticipantDto> addedParticipants = new ArrayList<>();
        
        for (User user : newParticipants) {
            if (!isUserParticipant(conversation, user.getUserId())) {
                ConversationParticipant participant = addParticipant(conversation, user, memberRole);
                addedParticipants.add(chatMapper.toParticipantDto(participant));
                
                // Send system message
                messageService.createAndSendSystemMessage(
                    conversationId,
                    String.format("%s was added to the conversation", user.getUsername())
                );
            }
        }
        
        // Update participant count
        updateConversationStats(conversationId);
        
        log.info("Added {} participants to conversation {}", addedParticipants.size(), conversationId);
        return addedParticipants;
    }

    @Override
    public void removeParticipant(Integer conversationId, Integer userIdToRemove, UserDetailsImpl currentUser) {
        log.info("Removing participant {} from conversation {} by user {}", userIdToRemove, conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        
        // Users can always remove themselves
        if (!userIdToRemove.equals(currentUser.getId())) {
            validateConversationPermission(conversation, currentUser, "remove_participants");
        }
        
        ConversationParticipant participantToRemove = getParticipantEntity(conversation, userIdToRemove);
        participantToRemove.setActive(false);
        participantRepository.save(participantToRemove);
        
        // Send system message
        User removedUser = getUserEntity(userIdToRemove);
        messageService.createAndSendSystemMessage(
            conversationId,
            String.format("%s was removed from the conversation", removedUser.getUsername())
        );
        
        // Update participant count
        updateConversationStats(conversationId);
        
        log.info("Participant {} removed from conversation {}", userIdToRemove, conversationId);
    }

    @Override
    public void leaveConversation(Integer conversationId, UserDetailsImpl currentUser) {
        log.info("User {} leaving conversation {}", currentUser.getId(), conversationId);
        removeParticipant(conversationId, currentUser.getId(), currentUser);
    }

    @Override
    public ParticipantDto updateParticipantRole(Integer conversationId, Integer userIdToUpdate, Integer roleId, UserDetailsImpl currentUser) {
        log.info("Updating role for user {} in conversation {} by user {}", userIdToUpdate, conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationPermission(conversation, currentUser, "manage_roles");
        
        ConversationParticipant participant = getParticipantEntity(conversation, userIdToUpdate);
        ConversationRole newRole = getRoleEntity(roleId);
        
        participant.setRole(newRole);
        ConversationParticipant updatedParticipant = participantRepository.save(participant);
        
        log.info("Role updated for user {} in conversation {}", userIdToUpdate, conversationId);
        return chatMapper.toParticipantDto(updatedParticipant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipantDto> getConversationParticipants(Integer conversationId, UserDetailsImpl currentUser) {
        log.debug("Fetching participants for conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationMembership(conversation, currentUser);
        
        // Use repository instead of entity collection
        List<ConversationParticipant> participants = participantRepository
            .findByConversationAndIsActiveTrue(conversation);
        
        return participants.stream()
                .map(chatMapper::toParticipantDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantDto getMyParticipantInfo(Integer conversationId, UserDetailsImpl currentUser) {
        log.debug("Fetching participant info for user {} in conversation {}", currentUser.getId(), conversationId);
        
        Conversation conversation = getConversationEntity(conversationId);
        ConversationParticipant participant = getParticipantEntity(conversation, currentUser.getId());
        
        return chatMapper.toParticipantDto(participant);
    }

    // ========== Moderation Actions ==========

    @Override
    public ParticipantDto muteParticipant(Integer conversationId, Integer userIdToMute, MuteDuration duration, UserDetailsImpl currentUser) {
        log.info("Muting user {} in conversation {} by user {}", userIdToMute, conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationPermission(conversation, currentUser, "mute_participants");
        
        ConversationParticipant participant = getParticipantEntity(conversation, userIdToMute);
        participant.setMuted(true);
        participant.setMutedUntil(calculateMuteUntil(duration));
        
        ConversationParticipant updatedParticipant = participantRepository.save(participant);
        
        log.info("User {} muted in conversation {} until {}", userIdToMute, conversationId, updatedParticipant.getMutedUntil());
        return chatMapper.toParticipantDto(updatedParticipant);
    }

    @Override
    public ParticipantDto unmuteParticipant(Integer conversationId, Integer userIdToUnmute, UserDetailsImpl currentUser) {
        log.info("Unmuting user {} in conversation {} by user {}", userIdToUnmute, conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationPermission(conversation, currentUser, "mute_participants");
        
        ConversationParticipant participant = getParticipantEntity(conversation, userIdToUnmute);
        participant.setMuted(false);
        participant.setMutedUntil(null);
        
        ConversationParticipant updatedParticipant = participantRepository.save(participant);
        
        log.info("User {} unmuted in conversation {}", userIdToUnmute, conversationId);
        return chatMapper.toParticipantDto(updatedParticipant);
    }

    @Override
    public void banParticipant(Integer conversationId, Integer userIdToBan, String reason, UserDetailsImpl currentUser) {
        log.info("Banning user {} from conversation {} by user {}", userIdToBan, conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationPermission(conversation, currentUser, "ban_participants");
        
        // First remove the participant
        removeParticipant(conversationId, userIdToBan, currentUser);
        
        // Then add to banned list (you'll need to implement this entity)
        // For now, we'll just log it
        log.info("User {} banned from conversation {} for reason: {}", userIdToBan, conversationId, reason);
    }

    @Override
    public void unbanParticipant(Integer conversationId, Integer userIdToUnban, UserDetailsImpl currentUser) {
        log.info("Unbanning user {} from conversation {} by user {}", userIdToUnban, conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationPermission(conversation, currentUser, "ban_participants");
        
        // Remove from banned list (you'll need to implement this)
        log.info("User {} unbanned from conversation {}", userIdToUnban, conversationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannedUserDto> getBannedParticipants(Integer conversationId, UserDetailsImpl currentUser) {
        log.debug("Fetching banned participants for conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationPermission(conversation, currentUser, "ban_participants");
        
        // Implement banned users retrieval
        return List.of(); // Placeholder
    }

    // ========== Utility Methods ==========

    @Override
    @Transactional(readOnly = true)
    public boolean directConversationExists(Integer user1Id, Integer user2Id) {
        return findExistingDirectConversation(
            getUserEntity(user1Id), 
            getUserEntity(user2Id)
        ).isPresent();
    }

    @Override
    public void updateConversationStats(Integer conversationId) {
        Conversation conversation = getConversationEntity(conversationId);
        
        long activeParticipantCount = participantRepository.countByConversationAndIsActive(conversation, true);
        conversation.setParticipantCount((int) activeParticipantCount);
        
        conversationRepository.save(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasConversationPermission(UserDetailsImpl user, Integer conversationId, String permission) {
        try {
            Conversation conversation = getConversationEntity(conversationId);
            return permissionService.hasPermission(
                getUserEntity(user.getId()), 
                permission, 
                conversation
            );
        } catch (Exception e) {
            log.warn("Permission check failed for user {} in conversation {}: {}", user.getId(), conversationId, e.getMessage());
            return false;
        }
    }

    // ========== Private Helper Methods ==========

    private User getUserEntity(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private Conversation getConversationEntity(Integer conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));
    }

    private ConversationRole getRoleEntity(Integer roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation role not found with id: " + roleId));
    }

    private ConversationParticipant getParticipantEntity(Conversation conversation, Integer userId) {
        return participantRepository.findByConversationAndUser_UserId(conversation, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found in conversation"));
    }

    private Optional<Conversation> findExistingDirectConversation(User user1, User user2) {
        return conversationRepository.findDirectConversationBetweenUsers(user1.getUserId(), user2.getUserId());
    }

    private ConversationRole getDefaultConversationRole() {
        return roleRepository.findByNameAndConversationIsNull(DefaultConversationRoles.CONVERSATION_MEMBER.getName())
                .orElseThrow(() -> new IllegalStateException("Default conversation role not found"));
    }

    private ConversationRole getConversationAdminRole() {
        return roleRepository.findByNameAndConversationIsNull(DefaultConversationRoles.CONVERSATION_ADMIN.getName())
                .orElseThrow(() -> new IllegalStateException("Conversation admin role not found"));
    }

    private ConversationParticipant addParticipant(Conversation conversation, User user, ConversationRole role) {
    // Create and set the composite key
    ConversationParticipant.ConversationParticipantId id = 
        new ConversationParticipant.ConversationParticipantId(user.getUserId(), conversation.getConversationId());
    
    ConversationParticipant participant = new ConversationParticipant();
    participant.setId(id); // Set the composite key
    participant.setUser(user);
    participant.setConversation(conversation);
    participant.setRole(role);
    participant.setJoinedAt(Instant.now());
    participant.setActive(true);
    participant.setNotificationsEnabled(true);
    
    return participantRepository.save(participant);
}

    private boolean isUserParticipant(Conversation conversation, Integer userId) {
        return participantRepository.existsByConversationAndUser_UserIdAndIsActive(conversation, userId, true);
    }

    private void validateConversationMembership(Conversation conversation, UserDetailsImpl user) {
        if (!isUserParticipant(conversation, user.getId())) {
            throw new AccessDeniedException("You are not a member of this conversation");
        }
    }

    private void validateConversationPermission(Conversation conversation, UserDetailsImpl user, String permission) {
        if (!hasConversationPermission(user, conversation.getConversationId(), permission)) {
            throw new AccessDeniedException("Insufficient permissions for this action");
        }
    }

    private void validateAdminRole(Conversation conversation, UserDetailsImpl user) {
        ConversationParticipant participant = getParticipantEntity(conversation, user.getId());
        if (!participant.getRole().getName().equals(DefaultConversationRoles.CONVERSATION_ADMIN.getName())) {
            throw new AccessDeniedException("Admin role required for this action");
        }
    }

        private Set<User> validateAndGetParticipants(List<Integer> participantIds, User currentUser) {
        if (participantIds == null || participantIds.isEmpty()) {
            return new HashSet<>();
        }
        
        // Use the new repository method
        Set<User> participants = new HashSet<>(userRepository.findByUserIdIn(participantIds));
        
        // Remove current user if present
        if (currentUser != null) {
            participants.removeIf(user -> user.getUserId().equals(currentUser.getUserId()));
        }
        
        // Validate that all requested participants were found
        if (participants.size() != participantIds.size() - (currentUser != null ? 1 : 0)) {
            throw new ResourceNotFoundException("One or more participants not found");
        }
        
        // Additional validation: ensure all participants are active
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
        // Implement message permission validation based on user settings
        // This could check if receiver allows messages from sender
        // For now, we'll assume it's always allowed
    }

    private University getUniversityContext(User user1, User user2) {
    // We'll implement this later in UniversityService
    // For now, return null or get from the first user's primary university
    return null; // Temporary - will be implemented in service layer
}


    private String generateDirectConversationTitle(User user1, User user2) {
        return String.format("%s and %s", user1.getUsername(), user2.getUsername());
    }

    private Instant calculateMuteUntil(MuteDuration duration) {
        return switch (duration) {
            case ONE_HOUR -> Instant.now().plus(1, ChronoUnit.HOURS);
            case EIGHT_HOURS -> Instant.now().plus(8, ChronoUnit.HOURS);
            case ONE_DAY -> Instant.now().plus(1, ChronoUnit.DAYS);
            case ONE_WEEK -> Instant.now().plus(7, ChronoUnit.DAYS);
            case PERMANENT -> Instant.now().plus(100, ChronoUnit.YEARS); // Effectively permanent
        };
    }

    // Enum for mute durations
    public enum MuteDuration {
        ONE_HOUR,
        EIGHT_HOURS, 
        ONE_DAY,
        ONE_WEEK,
        PERMANENT
    }
}