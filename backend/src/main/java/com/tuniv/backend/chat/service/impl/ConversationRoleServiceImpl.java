package com.tuniv.backend.chat.service.impl;

import org.springframework.stereotype.Service;

import com.tuniv.backend.chat.model.ConversationRole;
import com.tuniv.backend.chat.model.DefaultConversationRoles;
import com.tuniv.backend.chat.repository.ConversationRoleRepository;
import com.tuniv.backend.chat.service.ConversationRoleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConversationRoleServiceImpl implements ConversationRoleService {
    
    private final ConversationRoleRepository roleRepository;
    
    @Override
    public ConversationRole getDefaultConversationRole() {
        return roleRepository.findByNameAndConversationIsNull(DefaultConversationRoles.CONVERSATION_MEMBER.getName())
                .orElseThrow(() -> new IllegalStateException("Default conversation role not found"));
    }
    
    @Override
    public ConversationRole getConversationAdminRole() {
        return roleRepository.findByNameAndConversationIsNull(DefaultConversationRoles.CONVERSATION_ADMIN.getName())
                .orElseThrow(() -> new IllegalStateException("Conversation admin role not found"));
    }

    @Override
    public ConversationRole getConversationModeratorRole() {
        return roleRepository.findByNameAndConversationIsNull(DefaultConversationRoles.CONVERSATION_MODERATOR.getName())
                .orElseThrow(() -> new IllegalStateException("Conversation moderator role not found"));
    }
    
    @Override
    public ConversationRole getRoleByName(String roleName) {
        return roleRepository.findByNameAndConversationIsNull(roleName)
                .orElseThrow(() -> new IllegalStateException("Conversation role not found: " + roleName));
    }
}