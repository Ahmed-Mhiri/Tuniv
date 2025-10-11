package com.tuniv.backend.authorization.service;

import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.user.model.User;

import java.util.Set;

public interface PermissionService {
    
    /**
     * Checks if a user has a specific permission related to a target resource
     */
    boolean hasPermission(User user, String permissionName, Object targetResource);
    
    /**
     * Checks for a global platform or university permission not tied to a specific object
     */
    boolean hasPermission(User user, String permissionName);
    
    /**
     * Gathers all permissions for a user within the context of a specific community
     */
    Set<String> getCommunityPermissions(User user, Community community);
    
    /**
     * Gathers all permissions for a user within a specific university
     */
    Set<String> getUniversityPermissions(User user, University university);
    
    /**
     * Gathers all permissions for a user within a specific conversation
     */
    Set<String> getConversationPermissions(User user, Conversation conversation);
    
    /**
     * Check if user can edit a specific post (own or any based on permissions)
     */
    boolean canEditPost(User user, Post post);
    
    /**
     * Check if user can delete a specific post (own or any based on permissions)
     */
    boolean canDeletePost(User user, Post post);
}