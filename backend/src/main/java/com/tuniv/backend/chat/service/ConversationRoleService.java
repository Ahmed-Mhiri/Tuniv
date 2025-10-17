package com.tuniv.backend.chat.service;

import com.tuniv.backend.chat.model.ConversationRole;

public interface ConversationRoleService {
    ConversationRole getDefaultConversationRole();
    ConversationRole getConversationAdminRole();
    ConversationRole getRoleByName(String roleName);
    ConversationRole getConversationModeratorRole();
}