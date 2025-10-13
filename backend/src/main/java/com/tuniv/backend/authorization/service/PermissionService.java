package com.tuniv.backend.authorization.service;

import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.user.model.User;

import java.util.Set;

public interface PermissionService {
    
    /**
     * Primary method: Checks if a user has a specific permission related to a target resource using userId
     */
    boolean hasPermission(Integer userId, String permissionName, Object targetResource);
    
    /**
     * Legacy method: Checks if a user has a specific permission related to a target resource using User object
     */
    boolean hasPermission(User user, String permissionName, Object targetResource);
    
    /**
     * Primary method: Checks for a global platform or university permission not tied to a specific object using userId
     */
    boolean hasPermission(Integer userId, String permissionName);
    
    /**
     * Legacy method: Checks for a global platform or university permission not tied to a specific object using User object
     */
    boolean hasPermission(User user, String permissionName);
    
    /**
     * Primary method: Gathers all permissions for a user within the context of a specific community using userId
     */
    Set<String> getCommunityPermissions(Integer userId, Community community);
    
    /**
     * Legacy method: Gathers all permissions for a user within the context of a specific community using User object
     */
    Set<String> getCommunityPermissions(User user, Community community);
    
    /**
     * Primary method: Gathers all permissions for a user within a specific university using userId
     */
    Set<String> getUniversityPermissions(Integer userId, University university);
    
    /**
     * Legacy method: Gathers all permissions for a user within a specific university using User object
     */
    Set<String> getUniversityPermissions(User user, University university);
    
    /**
     * Primary method: Gathers all permissions for a user within a specific conversation using userId
     */
    Set<String> getConversationPermissions(Integer userId, Conversation conversation);
    
    /**
     * Legacy method: Gathers all permissions for a user within a specific conversation using User object
     */
    Set<String> getConversationPermissions(User user, Conversation conversation);
    
    /**
     * Primary method: Check if user can edit a specific post (own or any based on permissions) using userId
     */
    boolean canEditPost(Integer userId, Post post);
    
    /**
     * Legacy method: Check if user can edit a specific post (own or any based on permissions) using User object
     */
    boolean canEditPost(User user, Post post);
    
    /**
     * Primary method: Check if user can delete a specific post (own or any based on permissions) using userId
     */
    boolean canDeletePost(Integer userId, Post post);
    
    /**
     * Legacy method: Check if user can delete a specific post (own or any based on permissions) using User object
     */
    boolean canDeletePost(User user, Post post);
}