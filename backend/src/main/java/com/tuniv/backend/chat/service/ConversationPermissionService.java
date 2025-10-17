package com.tuniv.backend.chat.service;

import com.tuniv.backend.config.security.services.UserDetailsImpl;

public interface ConversationPermissionService {

    /**
     * Checks if a user has a specific permission for a given conversation.
     * Throws an exception if the permission is not granted.
     * * @param userId The ID of the user to check.
     * @param conversationId The ID of the conversation.
     * @param permission A string representing the permission (e.g., "MANAGE_PARTICIPANTS", "SEND_MESSAGES").
     */
    void checkPermission(Integer userId, Integer conversationId, String permission);

    /**
     * Overloaded method for convenience when working with UserDetailsImpl objects.
     */
    void checkPermission(UserDetailsImpl currentUser, Integer conversationId, String permission);

    /**
     * Checks if a user is a member of the conversation.
     * Throws an exception if they are not.
     * * @param userId The ID of the user.
     * @param conversationId The ID of the conversation.
     */
    void checkMembership(Integer userId, Integer conversationId);
}