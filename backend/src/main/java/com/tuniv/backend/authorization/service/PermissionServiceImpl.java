package com.tuniv.backend.authorization.service;

import com.tuniv.backend.authorization.model.Permission;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.model.ConversationRole;
import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.community.model.CommunityMembership;
import com.tuniv.backend.community.model.CommunityRole;
import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.qa.model.Reply;
import com.tuniv.backend.qa.model.Topic;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.university.model.UniversityMembership;
import com.tuniv.backend.university.model.UniversityRole;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.community.repository.CommunityMembershipRepository;
import com.tuniv.backend.university.repository.UniversityMembershipRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private final UniversityMembershipRepository universityMembershipRepository;
    private final CommunityMembershipRepository communityMembershipRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    // ============ PRIMARY METHODS (using userId) ============

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(Integer userId, String permissionName, Object targetResource) {
        if (userId == null) {
            return false;
        }

        // Fetch user to check platform admin status
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found for permission check: " + userId));
        
        // Platform admin bypasses all permission checks
        if (user.isPlatformAdmin()) {
            return true;
        }

        // Determine context and check permissions
        if (targetResource instanceof University) {
            return hasUniversityPermission(userId, permissionName, (University) targetResource);
        } else if (targetResource instanceof Community) {
            return hasCommunityPermission(userId, permissionName, (Community) targetResource);
        } else if (targetResource instanceof Conversation) {
            return hasConversationPermission(userId, permissionName, (Conversation) targetResource);
        } else if (targetResource instanceof Post) {
            return hasPostPermission(userId, permissionName, (Post) targetResource);
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(Integer userId, String permissionName) {
        if (userId == null) {
            return false;
        }

        // Fetch user to check platform admin status
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found for permission check: " + userId));
        
        if (user.isPlatformAdmin()) {
            return true;
        }

        // For platform-level permissions, check all university memberships
        return universityMembershipRepository.findByUser_UserId(userId)
                .stream()
                .anyMatch(membership -> hasPermissionInRole(permissionName, membership.getRole()));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getCommunityPermissions(Integer userId, Community community) {
        Set<String> permissions = new HashSet<>();
        
        if (userId == null || community == null) {
            return permissions;
        }

        // Check community membership using userId
        Optional<CommunityMembership> membership = communityMembershipRepository
                .findByUser_UserIdAndCommunity(userId, community);
        
        if (membership.isPresent() && membership.get().isActive()) {
            CommunityRole role = membership.get().getRole();
            permissions.addAll(role.getPermissions().stream()
                    .map(Permission::getName)
                    .collect(Collectors.toSet()));
        }

        // Add university-level permissions that apply to communities
        if (community.getUniversity() != null) {
            permissions.addAll(getUniversityPermissions(userId, community.getUniversity()));
        }

        return permissions;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getUniversityPermissions(Integer userId, University university) {
        Set<String> permissions = new HashSet<>();
        
        if (userId == null || university == null) {
            return permissions;
        }

        // Check university membership using userId
        Optional<UniversityMembership> membership = universityMembershipRepository
                .findByUser_UserIdAndUniversity(userId, university);
        
        if (membership.isPresent() && membership.get().isActive()) {
            UniversityRole role = membership.get().getRole();
            permissions.addAll(role.getPermissions().stream()
                    .map(Permission::getName)
                    .collect(Collectors.toSet()));
        }

        return permissions;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getConversationPermissions(Integer userId, Conversation conversation) {
        Set<String> permissions = new HashSet<>();
        
        if (userId == null || conversation == null) {
            return permissions;
        }

        // Check conversation participation using userId
        Optional<ConversationParticipant> participant = conversationParticipantRepository
                .findByUser_UserIdAndConversation(userId, conversation);
        
        if (participant.isPresent() && participant.get().isActive()) {
            ConversationRole role = participant.get().getRole();
            permissions.addAll(role.getPermissions().stream()
                    .map(Permission::getName)
                    .collect(Collectors.toSet()));
        }

        return permissions;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canEditPost(Integer userId, Post post) {
        if (userId == null || post == null) {
            return false;
        }

        // Fetch user to check if they are the author
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found for post edit check: " + userId));

        // User can always edit their own posts if they have the permission
        if (post.getAuthor().equals(user)) {
            return hasPermission(userId, ContentPermissions.POST_EDIT_OWN.getName(), post);
        }

        // Check for permission to edit any post
        return hasPermission(userId, ContentPermissions.POST_EDIT_ANY.getName(), post);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canDeletePost(Integer userId, Post post) {
        if (userId == null || post == null) {
            return false;
        }

        // Fetch user to check if they are the author
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found for post delete check: " + userId));

        // User can always delete their own posts if they have the permission
        if (post.getAuthor().equals(user)) {
            return hasPermission(userId, ContentPermissions.POST_DELETE_OWN.getName(), post);
        }

        // Check for permission to delete any post
        return hasPermission(userId, ContentPermissions.POST_DELETE_ANY.getName(), post);
    }

    // ============ LEGACY METHODS (using User object) - DELEGATE TO PRIMARY METHODS ============

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(User user, String permissionName, Object targetResource) {
        if (user == null) return false;
        return hasPermission(user.getUserId(), permissionName, targetResource);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(User user, String permissionName) {
        if (user == null) return false;
        return hasPermission(user.getUserId(), permissionName);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getCommunityPermissions(User user, Community community) {
        if (user == null) return new HashSet<>();
        return getCommunityPermissions(user.getUserId(), community);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getUniversityPermissions(User user, University university) {
        if (user == null) return new HashSet<>();
        return getUniversityPermissions(user.getUserId(), university);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getConversationPermissions(User user, Conversation conversation) {
        if (user == null) return new HashSet<>();
        return getConversationPermissions(user.getUserId(), conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canEditPost(User user, Post post) {
        if (user == null) return false;
        return canEditPost(user.getUserId(), post);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canDeletePost(User user, Post post) {
        if (user == null) return false;
        return canDeletePost(user.getUserId(), post);
    }

    // ============ PRIVATE HELPER METHODS (using userId) ============

    // Enhanced Post Permission Logic
    private boolean hasPostPermission(Integer userId, String permissionName, Post post) {
        // Determine context from post
        Community communityContext = extractCommunityFromPost(post);

        if (communityContext != null) {
            return hasCommunityPermission(userId, permissionName, communityContext);
        }

        // Check university context
        University universityContext = extractUniversityFromPost(post);
        if (universityContext != null) {
            return hasUniversityPermission(userId, permissionName, universityContext);
        }

        // Fallback for posts not tied to any specific context
        return hasPermission(userId, permissionName);
    }

    private boolean hasUniversityPermission(Integer userId, String permissionName, University university) {
        // Use optimized repository method if available
        if (universityMembershipRepository instanceof EnhancedUniversityMembershipRepository) {
            return ((EnhancedUniversityMembershipRepository) universityMembershipRepository)
                    .hasPermission(userId, university, permissionName);
        }
        
        // Fallback to original implementation with userId
        Optional<UniversityMembership> membership = universityMembershipRepository
                .findByUser_UserIdAndUniversity(userId, university);
        
        return membership.isPresent() && 
               membership.get().isActive() && 
               hasPermissionInRole(permissionName, membership.get().getRole());
    }

    private boolean hasCommunityPermission(Integer userId, String permissionName, Community community) {
        // Use optimized repository method if available
        if (communityMembershipRepository instanceof EnhancedCommunityMembershipRepository) {
            return ((EnhancedCommunityMembershipRepository) communityMembershipRepository)
                    .hasPermission(userId, community, permissionName);
        }
        
        // Fallback to original implementation with userId
        Optional<CommunityMembership> membership = communityMembershipRepository
                .findByUser_UserIdAndCommunity(userId, community);
        
        boolean hasCommunityPermission = membership.isPresent() && 
                membership.get().isActive() && 
                hasPermissionInRole(permissionName, membership.get().getRole());

        // Fall back to university permissions if community permission not found
        if (!hasCommunityPermission && community.getUniversity() != null) {
            return hasUniversityPermission(userId, permissionName, community.getUniversity());
        }

        return hasCommunityPermission;
    }

    private boolean hasConversationPermission(Integer userId, String permissionName, Conversation conversation) {
        // Use optimized repository method if available
        if (conversationParticipantRepository instanceof EnhancedConversationParticipantRepository) {
            return ((EnhancedConversationParticipantRepository) conversationParticipantRepository)
                    .hasPermission(userId, conversation, permissionName);
        }
        
        // Fallback to original implementation with userId
        Optional<ConversationParticipant> participant = conversationParticipantRepository
                .findByUser_UserIdAndConversation(userId, conversation);
        
        return participant.isPresent() && 
               participant.get().isActive() && 
               hasPermissionInRole(permissionName, participant.get().getRole());
    }

    // ============ UTILITY METHODS ============

    private Community extractCommunityFromPost(Post post) {
        // A Topic can belong to a Community
        if (post instanceof Topic) {
            return ((Topic) post).getCommunity();
        }
        // A Reply's context comes from its parent Topic
        else if (post instanceof Reply) {
            Topic parentTopic = ((Reply) post).getTopic();
            return parentTopic != null ? parentTopic.getCommunity() : null;
        }
        return null;
    }

    private University extractUniversityFromPost(Post post) {
        // Direct university context
        if (post.getUniversityContext() != null) {
            return post.getUniversityContext();
        }
        
        // For replies, check parent topic's university context
        if (post instanceof Reply) {
            Topic parentTopic = ((Reply) post).getTopic();
            return parentTopic != null ? parentTopic.getUniversityContext() : null;
        }
        
        return null;
    }

    private boolean hasPermissionInRole(String permissionName, Object role) {
        if (role instanceof UniversityRole) {
            return ((UniversityRole) role).getPermissions().stream()
                    .anyMatch(permission -> permission.getName().equals(permissionName));
        } else if (role instanceof CommunityRole) {
            return ((CommunityRole) role).getPermissions().stream()
                    .anyMatch(permission -> permission.getName().equals(permissionName));
        } else if (role instanceof ConversationRole) {
            return ((ConversationRole) role).getPermissions().stream()
                    .anyMatch(permission -> permission.getName().equals(permissionName));
        }
        return false;
    }
}