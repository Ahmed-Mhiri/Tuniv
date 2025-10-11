package com.tuniv.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.tuniv.backend.authorization.service.PermissionService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class PostAuthorizationService {

    private final PermissionService permissionService;
    private final UserRepository userRepository;

    /**
     * Checks if the current user is the author of the post.
     * Throws AccessDeniedException if they are not.
     * 
     * @param post The Post entity (Topic, Reply, or Message).
     * @param currentUser The currently authenticated user.
     */
    public void checkOwnership(Post post, UserDetailsImpl currentUser) {
        if (!post.getAuthor().getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access Denied: You are not the owner of this resource.");
        }
    }

    /**
     * Checks if the current user can edit the specified post.
     * Considers both ownership and permission-based access.
     * 
     * @param post The Post entity to edit
     * @param currentUser The currently authenticated user
     */
    public void checkEditPermission(Post post, UserDetailsImpl currentUser) {
        User user = getUserFromDetails(currentUser);
        
        if (!permissionService.canEditPost(user, post)) {
            throw new AccessDeniedException("Access Denied: You do not have permission to edit this post.");
        }
    }

    /**
     * Checks if the current user can delete the specified post.
     * Considers both ownership and permission-based access.
     * 
     * @param post The Post entity to delete
     * @param currentUser The currently authenticated user
     */
    public void checkDeletePermission(Post post, UserDetailsImpl currentUser) {
        User user = getUserFromDetails(currentUser);
        
        if (!permissionService.canDeletePost(user, post)) {
            throw new AccessDeniedException("Access Denied: You do not have permission to delete this post.");
        }
    }

    /**
     * Checks if the current user can create posts in the given context.
     * 
     * @param universityContext The university context (can be null)
     * @param currentUser The currently authenticated user
     */
    public void checkCreatePostPermission(com.tuniv.backend.university.model.University universityContext, 
                                         UserDetailsImpl currentUser) {
        User user = getUserFromDetails(currentUser);
        
        if (!permissionService.hasPermission(user, "post.topic.create", universityContext)) {
            throw new AccessDeniedException("Access Denied: You do not have permission to create posts in this context.");
        }
    }

    /**
     * Enhanced ownership check that allows moderators/admins to bypass ownership requirement
     * for specific actions (like viewing moderation tools).
     * 
     * @param post The Post entity
     * @param currentUser The currently authenticated user
     * @param requireOwnership If true, only owners can access; if false, moderators can too
     */
    public void checkOwnershipOrModerator(Post post, UserDetailsImpl currentUser, boolean requireOwnership) {
        User user = getUserFromDetails(currentUser);
        
        // Always allow if user is the owner
        if (post.getAuthor().getUserId().equals(currentUser.getId())) {
            return;
        }
        
        // If ownership is strictly required and user is not owner, deny access
        if (requireOwnership) {
            throw new AccessDeniedException("Access Denied: You are not the owner of this resource.");
        }
        
        // For non-owners, check if they have moderator permissions
        if (!permissionService.hasPermission(user, "post.edit.any", post)) {
            throw new AccessDeniedException("Access Denied: You do not have moderator privileges for this resource.");
        }
    }

    /**
     * Checks if user can pin/unpin a post (typically for topics in communities)
     * 
     * @param post The Post entity (usually a Topic)
     * @param currentUser The currently authenticated user
     */
    public void checkPinPermission(Post post, UserDetailsImpl currentUser) {
        User user = getUserFromDetails(currentUser);
        
        if (!permissionService.hasPermission(user, "community.topic.pin", post)) {
            throw new AccessDeniedException("Access Denied: You do not have permission to pin/unpin this post.");
        }
    }

    /**
     * Checks if user can lock/unlock a post (typically for topics)
     * 
     * @param post The Post entity (usually a Topic)
     * @param currentUser The currently authenticated user
     */
    public void checkLockPermission(Post post, UserDetailsImpl currentUser) {
        User user = getUserFromDetails(currentUser);
        
        if (!permissionService.hasPermission(user, "community.topic.lock", post)) {
            throw new AccessDeniedException("Access Denied: You do not have permission to lock/unlock this post.");
        }
    }

    /**
     * Checks if user can manage reports for a post
     * 
     * @param post The Post entity
     * @param currentUser The currently authenticated user
     */
    public void checkReportManagementPermission(Post post, UserDetailsImpl currentUser) {
        User user = getUserFromDetails(currentUser);
        
        // Try community report management first
        boolean hasPermission = permissionService.hasPermission(user, "community.report.manage", post) ||
                               permissionService.hasPermission(user, "university.report.manage", post);
        
        if (!hasPermission) {
            throw new AccessDeniedException("Access Denied: You do not have permission to manage reports for this post.");
        }
    }

    /**
     * Comprehensive post action authorization check
     * 
     * @param post The Post entity
     * @param currentUser The currently authenticated user
     * @param action The action to perform ("edit", "delete", "pin", "lock", "manage_reports")
     */
    public void checkPostAction(Post post, UserDetailsImpl currentUser, String action) {
        User user = getUserFromDetails(currentUser);
        
        switch (action.toLowerCase()) {
            case "edit":
                if (!permissionService.canEditPost(user, post)) {
                    throw new AccessDeniedException("Access Denied: You do not have permission to edit this post.");
                }
                break;
                
            case "delete":
                if (!permissionService.canDeletePost(user, post)) {
                    throw new AccessDeniedException("Access Denied: You do not have permission to delete this post.");
                }
                break;
                
            case "pin":
                if (!permissionService.hasPermission(user, "community.topic.pin", post)) {
                    throw new AccessDeniedException("Access Denied: You do not have permission to pin this post.");
                }
                break;
                
            case "lock":
                if (!permissionService.hasPermission(user, "community.topic.lock", post)) {
                    throw new AccessDeniedException("Access Denied: You do not have permission to lock this post.");
                }
                break;
                
            case "manage_reports":
                boolean canManageReports = permissionService.hasPermission(user, "community.report.manage", post) ||
                                         permissionService.hasPermission(user, "university.report.manage", post);
                if (!canManageReports) {
                    throw new AccessDeniedException("Access Denied: You do not have permission to manage reports for this post.");
                }
                break;
                
            default:
                throw new IllegalArgumentException("Unknown post action: " + action);
        }
    }

    /**
     * Utility method to convert UserDetailsImpl to User entity
     */
    private User getUserFromDetails(UserDetailsImpl userDetails) {
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new AccessDeniedException("User not found"));
    }
}