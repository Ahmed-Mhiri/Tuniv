package com.tuniv.backend.chat.service.impl;

import com.tuniv.backend.authorization.model.Permission;
import com.tuniv.backend.authorization.service.PermissionService;
import com.tuniv.backend.chat.dto.*;
import com.tuniv.backend.chat.mapper.ChatMapper;
import com.tuniv.backend.chat.model.*;
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.chat.repository.ConversationRepository;
import com.tuniv.backend.chat.repository.ConversationRoleRepository;
import com.tuniv.backend.chat.repository.DirectConversationLookupRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import com.tuniv.backend.chat.repository.MessageRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ConversationRoleRepository roleRepository;
    private final DirectConversationLookupRepository directConversationLookupRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final ChatMapper chatMapper;
    private final MessageService messageService;

    @Override
    @Transactional(readOnly = true)
    public boolean hasConversationPermission(Integer userId, Integer conversationId, String permission) {
        try {
            return permissionService.hasPermission(userId, permission, conversationId);
        } catch (Exception e) {
            log.warn("Permission check failed for user {} in conversation {}: {}", userId, conversationId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasConversationPermission(UserDetailsImpl user, Integer conversationId, String permission) {
        return hasConversationPermission(user.getId(), conversationId, permission);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasConversationPermission(User user, Integer conversationId, String permission) {
        return hasConversationPermission(user.getUserId(), conversationId, permission);
    }

    private void validateConversationPermission(Integer userId, Integer conversationId, String permission) {
        if (!hasConversationPermission(userId, conversationId, permission)) {
            throw new AccessDeniedException("Insufficient permissions for this action. Required: " + permission);
        }
    }

    private void validateConversationPermission(UserDetailsImpl user, Integer conversationId, String permission) {
        validateConversationPermission(user.getId(), conversationId, permission);
    }

    private void validateConversationPermission(User user, Integer conversationId, String permission) {
        validateConversationPermission(user.getUserId(), conversationId, permission);
    }

    @Override
    public ConversationDetailDto startDirectConversation(StartConversationRequestDto request, UserDetailsImpl currentUser) {
        log.info("Starting direct conversation between user {} and {}", currentUser.getId(), request.getTargetUserId());
        
        User currentUserEntity = getUserEntity(currentUser.getId());
        User targetUser = getUserEntity(request.getTargetUserId());
        
        Integer user1Id = Math.min(currentUserEntity.getUserId(), targetUser.getUserId());
        Integer user2Id = Math.max(currentUserEntity.getUserId(), targetUser.getUserId());
        
        Optional<DirectConversationLookup> existingLookup = directConversationLookupRepository
                .findByUser1IdAndUser2Id(user1Id, user2Id);
        
        if (existingLookup.isPresent()) {
            log.info("Direct conversation already exists: {}", existingLookup.get().getConversation().getConversationId());
            return chatMapper.toConversationDetailDto(existingLookup.get().getConversation(), currentUserEntity);
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
                return chatMapper.toConversationDetailDto(existingLookup.get().getConversation(), currentUserEntity);
            }
            throw new RuntimeException("Failed to create direct conversation due to race condition", e);
        }
        
        ConversationRole memberRole = getDefaultConversationRole();
        addParticipant(savedConversation, currentUserEntity, memberRole);
        addParticipant(savedConversation, targetUser, memberRole);
        
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
        
        Set<User> participants = validateAndGetParticipants(request.getParticipantIds(), currentUserEntity);
        
        Conversation conversation = new Conversation();
        conversation.setTitle(request.getTitle());
        conversation.setConversationType(ConversationType.GROUP);
        conversation.setParticipantCount(participants.size() + 1);
        
        Conversation savedConversation = conversationRepository.save(conversation);
        log.info("Created new group conversation: {}", savedConversation.getConversationId());
        
        ConversationRole adminRole = getConversationAdminRole();
        ConversationRole memberRole = getDefaultConversationRole();
        
        addParticipant(savedConversation, currentUserEntity, adminRole);
        
        participants.forEach(participant -> 
            addParticipant(savedConversation, participant, memberRole)
        );
        
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
        validateConversationPermission(currentUser.getId(), conversation.getConversationId(), "archive_conversation");
        
        conversation.setArchived(true);
        conversationRepository.save(conversation);
        
        log.info("Conversation {} archived successfully", conversationId);
    }

    @Override
    public void deleteConversation(Integer conversationId, UserDetailsImpl currentUser) {
        log.info("Deleting conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationPermission(currentUser.getId(), conversation.getConversationId(), "delete_conversation");
        
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
        validateConversationPermission(currentUser.getId(), conversation.getConversationId(), "archive_conversation");
        
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
    
    // Use the optimized bulk mapper instead of individual queries
    List<ConversationSummaryDto> dtos = chatMapper.toConversationSummaryDtoListOptimized(
        conversations.getContent(), currentUser.getId());
    
    return new PageImpl<>(dtos, pageable, conversations.getTotalElements());
}

    @Override
    @Transactional(readOnly = true)
    public ConversationDetailDto getConversationDetails(Integer conversationId, UserDetailsImpl currentUser) {
        log.debug("Fetching details for conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = conversationRepository.findWithParticipantsById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));
        
        validateConversationMembership(conversation, currentUser);
        
        User currentUserEntity = getUserEntity(currentUser.getId());
        
        List<Message> pinnedMessages = messageRepository.findByConversationAndPinnedTrueAndDeletedFalse(conversation);
        
        List<ConversationParticipant> participants = conversation.getParticipants().stream()
                .filter(ConversationParticipant::isActive)
                .collect(Collectors.toList());
        
        return chatMapper.toConversationDetailDtoOptimized(
            conversation, currentUserEntity, participants, pinnedMessages);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationSummaryDto> searchConversations(String query, UserDetailsImpl currentUser, Pageable pageable) {
        log.debug("Searching conversations with query '{}' for user {}", query, currentUser.getId());
        
        Page<Conversation> conversations = conversationRepository.searchConversations(query, pageable);
        
        if (conversations.isEmpty()) {
            return Page.empty(pageable);
        }
        
        List<Integer> conversationIds = conversations.getContent().stream()
                .map(Conversation::getConversationId)
                .collect(Collectors.toList());
        
        List<ConversationParticipant> participants = participantRepository
                .findByConversation_ConversationIdInAndUser_UserIdAndIsActiveTrue(conversationIds, currentUser.getId());
        
        Map<Integer, ConversationParticipant> participantMap = participants.stream()
                .collect(Collectors.toMap(
                    cp -> cp.getConversation().getConversationId(),
                    cp -> cp
                ));
        
        List<ConversationSummaryDto> dtos = conversations.getContent().stream()
                .map(conversation -> {
                    ConversationSummaryDto dto = toConversationSummaryDtoBasic(conversation);
                    
                    ConversationParticipant participant = participantMap.get(conversation.getConversationId());
                    if (participant != null) {
                        dto.setUnreadCount(participant.getUnreadCount());
                    } else {
                        dto.setUnreadCount(0);
                    }
                    
                    return dto;
                })
                .collect(Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(dtos, pageable, conversations.getTotalElements());
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

    @Override
    public ConversationDetailDto updateGroupInfo(Integer conversationId, UpdateGroupInfoRequest request, UserDetailsImpl currentUser) {
        log.info("Updating group info for conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationPermission(currentUser.getId(), conversation.getConversationId(), "edit_conversation_info");
        
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            conversation.setTitle(request.getTitle().trim());
        }
        
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
                participant.setMutedUntil(Instant.now().plus(24, ChronoUnit.HOURS));
            } else {
                participant.setMutedUntil(null);
            }
        }
        
        ConversationParticipant updatedParticipant = participantRepository.save(participant);
        
        log.info("Conversation settings updated for user {} in conversation {}", currentUser.getId(), conversationId);
        return chatMapper.toParticipantDto(updatedParticipant);
    }

    @Override
    public List<ParticipantDto> addParticipants(Integer conversationId, UpdateParticipantsRequest request, UserDetailsImpl currentUser) {
        log.info("Adding participants to conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationPermission(currentUser.getId(), conversation.getConversationId(), "add_participants");
        
        Set<User> newParticipants = validateAndGetParticipants(request.getUserIds(), null);
        ConversationRole memberRole = getDefaultConversationRole();
        
        List<ConversationParticipant> newParticipantEntities = new ArrayList<>();
        List<ParticipantDto> addedParticipants = new ArrayList<>();
        
        for (User user : newParticipants) {
            if (!isUserParticipant(conversation, user.getUserId())) {
                ConversationParticipant.ConversationParticipantId id = 
                    new ConversationParticipant.ConversationParticipantId(user.getUserId(), conversation.getConversationId());
                
                ConversationParticipant participant = new ConversationParticipant();
                participant.setId(id);
                participant.setUser(user);
                participant.setConversation(conversation);
                participant.setRole(memberRole);
                participant.setJoinedAt(Instant.now());
                participant.setActive(true);
                participant.setNotificationsEnabled(true);
                
                newParticipantEntities.add(participant);
            }
        }
        
        if (!newParticipantEntities.isEmpty()) {
            List<ConversationParticipant> savedParticipants = participantRepository.saveAll(newParticipantEntities);
            
            addedParticipants = savedParticipants.stream()
                    .map(chatMapper::toParticipantDto)
                    .collect(Collectors.toList());
            
            for (ConversationParticipant participant : savedParticipants) {
                messageService.createAndSendSystemMessage(
                    conversationId,
                    String.format("%s was added to the conversation", participant.getUser().getUsername())
                );
            }
        }
        
        updateConversationStats(conversationId);
        
        log.info("Added {} participants to conversation {}", addedParticipants.size(), conversationId);
        return addedParticipants;
    }

    @Override
    public void removeParticipant(Integer conversationId, Integer userIdToRemove, UserDetailsImpl currentUser) {
        log.info("Removing participant {} from conversation {} by user {}", userIdToRemove, conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        
        if (!userIdToRemove.equals(currentUser.getId())) {
            validateConversationPermission(currentUser.getId(), conversation.getConversationId(), "remove_participants");
        }
        
        ConversationParticipant participantToRemove = getParticipantEntity(conversation, userIdToRemove);
        participantToRemove.setActive(false);
        participantRepository.save(participantToRemove);
        
        User removedUser = getUserEntity(userIdToRemove);
        messageService.createAndSendSystemMessage(
            conversationId,
            String.format("%s was removed from the conversation", removedUser.getUsername())
        );
        
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
        validateConversationPermission(currentUser.getId(), conversation.getConversationId(), "manage_roles");
        
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

    @Override
    public ParticipantDto muteParticipant(Integer conversationId, Integer userIdToMute, MuteDuration duration, UserDetailsImpl currentUser) {
        log.info("Muting user {} in conversation {} by user {}", userIdToMute, conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationPermission(currentUser.getId(), conversation.getConversationId(), "mute_participants");
        
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
        validateConversationPermission(currentUser.getId(), conversation.getConversationId(), "mute_participants");
        
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
        validateConversationPermission(currentUser.getId(), conversation.getConversationId(), "ban_participants");
        
        removeParticipant(conversationId, userIdToBan, currentUser);
        
        log.info("User {} banned from conversation {} for reason: {}", userIdToBan, conversationId, reason);
    }

    @Override
    public void unbanParticipant(Integer conversationId, Integer userIdToUnban, UserDetailsImpl currentUser) {
        log.info("Unbanning user {} from conversation {} by user {}", userIdToUnban, conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationPermission(currentUser.getId(), conversation.getConversationId(), "ban_participants");
        
        log.info("User {} unbanned from conversation {}", userIdToUnban, conversationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannedUserDto> getBannedParticipants(Integer conversationId, UserDetailsImpl currentUser) {
        log.debug("Fetching banned participants for conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = getConversationEntity(conversationId);
        validateConversationPermission(currentUser.getId(), conversation.getConversationId(), "ban_participants");
        
        return List.of();
    }

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
        ConversationParticipant.ConversationParticipantId id = 
            new ConversationParticipant.ConversationParticipantId(user.getUserId(), conversation.getConversationId());
        
        ConversationParticipant participant = new ConversationParticipant();
        participant.setId(id);
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
        
        Set<User> participants = new HashSet<>(userRepository.findByUserIdIn(participantIds));
        
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
    }

    private University getUniversityContext(User user1, User user2) {
        return null;
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
            case PERMANENT -> Instant.now().plus(100, ChronoUnit.YEARS);
        };
    }

    private ConversationSummaryDto toConversationSummaryDtoBasic(Conversation conversation) {
        ConversationSummaryDto dto = new ConversationSummaryDto();
        dto.setConversationId(conversation.getConversationId());
        dto.setTitle(conversation.getTitle());
        dto.setConversationType(conversation.getConversationType().name());
        dto.setLastMessageBody(conversation.getLastMessageBody());
        dto.setLastMessageSentAt(conversation.getLastMessageSentAt());
        dto.setParticipantCount(conversation.getParticipantCount());
        dto.setArchived(conversation.isArchived());
        dto.setUpdatedAt(conversation.getUpdatedAt());

        if (conversation.getLastMessageAuthor() != null) {
            dto.setLastMessageAuthorId(conversation.getLastMessageAuthor().getUserId());
            dto.setLastMessageAuthorName(conversation.getLastMessageAuthor().getUsername());
        }

        return dto;
    }

    private ReadReceiptDto toReadReceiptDto(ConversationParticipant participant) {
        ReadReceiptDto dto = new ReadReceiptDto();
        dto.setUserId(participant.getUser().getUserId());
        dto.setUsername(participant.getUser().getUsername());
        dto.setProfilePhotoUrl(participant.getUser().getProfilePhotoUrl());
        dto.setConversationId(participant.getConversation().getConversationId());
        dto.setLastReadTimestamp(participant.getLastReadTimestamp());
        dto.setReadAt(participant.getLastReadTimestamp());
        
        return dto;
    }
}