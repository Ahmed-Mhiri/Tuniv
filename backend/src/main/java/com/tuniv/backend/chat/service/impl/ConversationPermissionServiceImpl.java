package com.tuniv.backend.chat.service.impl;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.authorization.service.PermissionService;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.service.ConversationPermissionService;
import com.tuniv.backend.chat.service.EntityFinderService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.user.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ConversationPermissionServiceImpl implements ConversationPermissionService {

    private final PermissionService permissionService;
    private final EntityFinderService entityFinderService;

    @Override
    public void checkPermission(Integer userId, Integer conversationId, String permission) {
        log.debug("Checking permission '{}' for user {} in conversation {}", permission, userId, conversationId);
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        
        if (!permissionService.hasPermission(userId, permission, conversation)) {
            throw new AccessDeniedException(
                String.format("User %s does not have permission '%s' for conversation %s", 
                    userId, permission, conversationId)
            );
        }
    }

    @Override
    public void checkPermission(UserDetailsImpl currentUser, Integer conversationId, String permission) {
        checkPermission(currentUser.getId(), conversationId, permission);
    }

    @Override
    public void checkMembership(Integer userId, Integer conversationId) {
        log.debug("Checking membership for user {} in conversation {}", userId, conversationId);
        
        Conversation conversation = entityFinderService.getConversationOrThrow(conversationId);
        User user = entityFinderService.getUserOrThrow(userId);
        
        boolean isMember = conversation.getParticipants().stream()
                .anyMatch(participant -> 
                    participant.getUser().getUserId().equals(userId) && 
                    participant.isActive()
                );
        
        if (!isMember) {
            throw new AccessDeniedException(
                String.format("User %s is not a member of conversation %s", userId, conversationId)
            );
        }
    }
}