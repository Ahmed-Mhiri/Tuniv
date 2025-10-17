package com.tuniv.backend.chat.service.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.chat.dto.ParticipantDto;
import com.tuniv.backend.chat.dto.UpdateConversationSettingsRequest;
import com.tuniv.backend.chat.dto.UpdateParticipantsRequest;
import com.tuniv.backend.chat.mapper.mapstruct.ParticipantMapper;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.model.ConversationRole;
import com.tuniv.backend.chat.model.MuteDuration;
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.chat.repository.ConversationRepository;
import com.tuniv.backend.chat.service.ChatEventPublisher;
import com.tuniv.backend.chat.service.ConversationPermissionService;
import com.tuniv.backend.chat.service.ConversationRoleService;
import com.tuniv.backend.chat.service.EntityFinderService;
import com.tuniv.backend.chat.service.ParticipantService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ParticipantServiceImpl implements ParticipantService {

    private final ConversationParticipantRepository participantRepository;
    private final ConversationRepository conversationRepository;
    private final ParticipantMapper participantMapper;
    private final ChatEventPublisher chatEventPublisher;
    private final EntityFinderService entityFinderService;
    private final ConversationPermissionService conversationPermissionService;
    private final ConversationRoleService conversationRoleService; // Added this dependency

    @Override
    public List<ParticipantDto> addParticipants(Integer conversationId, UpdateParticipantsRequest request, UserDetailsImpl currentUser) {
        log.info("Adding participants to conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        conversationPermissionService.checkPermission(currentUser, conversationId, "add_participants");
        
        List<User> newParticipants = validateAndGetParticipants(request.getUserIds());
        ConversationRole memberRole = getDefaultConversationRole(); // Now uses the service
        
        List<ConversationParticipant> newParticipantEntities = new ArrayList<>();
        List<ParticipantDto> addedParticipants = new ArrayList<>();
        
        for (User user : newParticipants) {
            if (!isUserParticipant(conversation, user.getUserId())) {
                ConversationParticipant participant = addParticipant(conversation, user, memberRole);
                newParticipantEntities.add(participant);
            }
        }
        
        if (!newParticipantEntities.isEmpty()) {
            addedParticipants = participantMapper.toParticipantDtoList(newParticipantEntities);
            
            for (ConversationParticipant participant : newParticipantEntities) {
                chatEventPublisher.publishSystemMessageRequested(
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
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        
        if (!userIdToRemove.equals(currentUser.getId())) {
            conversationPermissionService.checkPermission(currentUser, conversationId, "remove_participants");
        }
        
        ConversationParticipant participantToRemove = getParticipantEntity(conversation, userIdToRemove);
        participantToRemove.setActive(false);
        participantRepository.save(participantToRemove);
        
        User removedUser = entityFinderService.getUserOrThrow(userIdToRemove);
        
        chatEventPublisher.publishSystemMessageRequested(
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
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        conversationPermissionService.checkPermission(currentUser, conversationId, "manage_roles");
        
        ConversationParticipant participant = getParticipantEntity(conversation, userIdToUpdate);
        ConversationRole newRole = entityFinderService.getConversationRoleOrThrow(roleId);
        
        participant.setRole(newRole);
        ConversationParticipant updatedParticipant = participantRepository.save(participant);
        
        log.info("Role updated for user {} in conversation {}", userIdToUpdate, conversationId);
        return participantMapper.toParticipantDto(updatedParticipant);
    }

    @Override
    public ParticipantDto updateMyConversationSettings(Integer conversationId, UpdateConversationSettingsRequest request, UserDetailsImpl currentUser) {
        log.info("Updating conversation settings for user {} in conversation {}", currentUser.getId(), conversationId);
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
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
        return participantMapper.toParticipantDto(updatedParticipant);
    }

    @Override
    public ParticipantDto muteParticipant(Integer conversationId, Integer userIdToMute, MuteDuration duration, UserDetailsImpl currentUser) {
        log.info("Muting user {} in conversation {} by user {}", userIdToMute, conversationId, currentUser.getId());
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        conversationPermissionService.checkPermission(currentUser, conversationId, "mute_participants");
        
        ConversationParticipant participant = getParticipantEntity(conversation, userIdToMute);
        participant.setMuted(true);
        participant.setMutedUntil(calculateMuteUntil(duration));
        
        ConversationParticipant updatedParticipant = participantRepository.save(participant);
        
        log.info("User {} muted in conversation {} until {}", userIdToMute, conversationId, updatedParticipant.getMutedUntil());
        return participantMapper.toParticipantDto(updatedParticipant);
    }

    @Override
    public ParticipantDto unmuteParticipant(Integer conversationId, Integer userIdToUnmute, UserDetailsImpl currentUser) {
        log.info("Unmuting user {} in conversation {} by user {}", userIdToUnmute, conversationId, currentUser.getId());
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        conversationPermissionService.checkPermission(currentUser, conversationId, "mute_participants");
        
        ConversationParticipant participant = getParticipantEntity(conversation, userIdToUnmute);
        participant.setMuted(false);
        participant.setMutedUntil(null);
        
        ConversationParticipant updatedParticipant = participantRepository.save(participant);
        
        log.info("User {} unmuted in conversation {}", userIdToUnmute, conversationId);
        return participantMapper.toParticipantDto(updatedParticipant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipantDto> getConversationParticipants(Integer conversationId, UserDetailsImpl currentUser) {
        log.debug("Fetching participants for conversation {} by user {}", conversationId, currentUser.getId());
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        conversationPermissionService.checkMembership(currentUser.getId(), conversationId);
        
        List<ConversationParticipant> participants = participantRepository
            .findByConversationAndIsActiveTrue(conversation);
        
        return participantMapper.toParticipantDtoList(participants);
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantDto getMyParticipantInfo(Integer conversationId, UserDetailsImpl currentUser) {
        log.debug("Fetching participant info for user {} in conversation {}", currentUser.getId(), conversationId);
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        ConversationParticipant participant = getParticipantEntity(conversation, currentUser.getId());
        
        return participantMapper.toParticipantDto(participant);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationParticipant getParticipantEntity(Conversation conversation, Integer userId) {
        return participantRepository.findByConversationAndUser_UserId(conversation, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found in conversation"));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserParticipant(Conversation conversation, Integer userId) {
        return participantRepository.existsByConversationAndUser_UserIdAndIsActive(conversation, userId, true);
    }

    @Override
    public void validateConversationMembership(Conversation conversation, UserDetailsImpl user) {
        if (!isUserParticipant(conversation, user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You are not a member of this conversation");
        }
    }

    @Override
    public ConversationParticipant addParticipant(Conversation conversation, User user, ConversationRole role) {
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

    @Override
    public void updateConversationStats(Integer conversationId) {
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        
        long activeParticipantCount = countActiveParticipants(conversation);
        conversation.setParticipantCount((int) activeParticipantCount);
        
        // Save the updated conversation
        conversationRepository.save(conversation);
        
        log.debug("Updated conversation stats for conversation {}: {} active participants", 
                 conversationId, activeParticipantCount);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveParticipants(Conversation conversation) {
        return participantRepository.countByConversationAndIsActive(conversation, true);
    }

    @Override
    public ConversationRole getDefaultConversationRole() {
        // Now uses the ConversationRoleService instead of directly using the repository
        return conversationRoleService.getDefaultConversationRole();
    }

    @Override
    public ConversationRole getConversationAdminRole() {
        // Now uses the ConversationRoleService instead of directly using the repository
        return conversationRoleService.getConversationAdminRole();
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationRole getConversationModeratorRole() {
        // Added this method to use the ConversationRoleService
        return conversationRoleService.getConversationModeratorRole();
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationRole getRoleByName(String roleName) {
        // Added this method to use the ConversationRoleService
        return conversationRoleService.getRoleByName(roleName);
    }

    // ========== Private Helper Methods ==========

    private List<User> validateAndGetParticipants(List<Integer> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<User> participants = entityFinderService.getUsersByIds(participantIds);
        
        if (participants.size() != participantIds.size()) {
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

    private Instant calculateMuteUntil(MuteDuration duration) {
        return switch (duration) {
            case ONE_HOUR -> Instant.now().plus(1, ChronoUnit.HOURS);
            case EIGHT_HOURS -> Instant.now().plus(8, ChronoUnit.HOURS);
            case ONE_DAY -> Instant.now().plus(1, ChronoUnit.DAYS);
            case ONE_WEEK -> Instant.now().plus(7, ChronoUnit.DAYS);
            case PERMANENT -> Instant.now().plus(100, ChronoUnit.YEARS);
        };
    }
}