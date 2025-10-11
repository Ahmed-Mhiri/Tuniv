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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private final UniversityMembershipRepository universityMembershipRepository;
    private final CommunityMembershipRepository communityMembershipRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(User user, String permissionName, Object targetResource) {
        if (user == null) {
            return false;
        }

        // Platform admin bypasses all permission checks
        if (user.isPlatformAdmin()) {
            return true;
        }

        // Determine context and check permissions
        if (targetResource instanceof University) {
            return hasUniversityPermission(user, permissionName, (University) targetResource);
        } else if (targetResource instanceof Community) {
            return hasCommunityPermission(user, permissionName, (Community) targetResource);
        } else if (targetResource instanceof Conversation) {
            return hasConversationPermission(user, permissionName, (Conversation) targetResource);
        } else if (targetResource instanceof Post) {
            return hasPostPermission(user, permissionName, (Post) targetResource);
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(User user, String permissionName) {
        if (user == null) {
            return false;
        }

        if (user.isPlatformAdmin()) {
            return true;
        }

        // For platform-level permissions, check all university memberships
        return universityMembershipRepository.findByUser(user)
                .stream()
                .anyMatch(membership -> hasPermissionInRole(permissionName, membership.getRole()));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getCommunityPermissions(User user, Community community) {
        Set<String> permissions = new HashSet<>();
        
        if (user == null || community == null) {
            return permissions;
        }

        // Check community membership
        Optional<CommunityMembership> membership = communityMembershipRepository
                .findByUserAndCommunity(user, community);
        
        if (membership.isPresent() && membership.get().isActive()) {
            CommunityRole role = membership.get().getRole();
            permissions.addAll(role.getPermissions().stream()
                    .map(Permission::getName)
                    .collect(Collectors.toSet()));
        }

        // Add university-level permissions that apply to communities
        if (community.getUniversity() != null) {
            permissions.addAll(getUniversityPermissions(user, community.getUniversity()));
        }

        return permissions;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getUniversityPermissions(User user, University university) {
        Set<String> permissions = new HashSet<>();
        
        if (user == null || university == null) {
            return permissions;
        }

        // Check university membership
        Optional<UniversityMembership> membership = universityMembershipRepository
                .findByUserAndUniversity(user, university);
        
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
    public Set<String> getConversationPermissions(User user, Conversation conversation) {
        Set<String> permissions = new HashSet<>();
        
        if (user == null || conversation == null) {
            return permissions;
        }

        // Check conversation participation
        Optional<ConversationParticipant> participant = conversationParticipantRepository
                .findByUserAndConversation(user, conversation);
        
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
    public boolean canEditPost(User user, Post post) {
        if (user == null || post == null) {
            return false;
        }

        // User can always edit their own posts if they have the permission
        if (post.getAuthor().equals(user)) {
            return hasPermission(user, ContentPermissions.POST_EDIT_OWN.getName(), post);
        }

        // Check for permission to edit any post
        return hasPermission(user, ContentPermissions.POST_EDIT_ANY.getName(), post);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canDeletePost(User user, Post post) {
        if (user == null || post == null) {
            return false;
        }

        // User can always delete their own posts if they have the permission
        if (post.getAuthor().equals(user)) {
            return hasPermission(user, ContentPermissions.POST_DELETE_OWN.getName(), post);
        }

        // Check for permission to delete any post
        return hasPermission(user, ContentPermissions.POST_DELETE_ANY.getName(), post);
    }

    // Enhanced Post Permission Logic
    private boolean hasPostPermission(User user, String permissionName, Post post) {
        // Determine context from post
        Community communityContext = extractCommunityFromPost(post);

        if (communityContext != null) {
            return hasCommunityPermission(user, permissionName, communityContext);
        }

        // Check university context
        University universityContext = extractUniversityFromPost(post);
        if (universityContext != null) {
            return hasUniversityPermission(user, permissionName, universityContext);
        }

        // Fallback for posts not tied to any specific context
        return hasPermission(user, permissionName);
    }

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

    // Private helper methods
    private boolean hasUniversityPermission(User user, String permissionName, University university) {
        // Use optimized repository method if available
        if (universityMembershipRepository instanceof EnhancedUniversityMembershipRepository) {
            return ((EnhancedUniversityMembershipRepository) universityMembershipRepository)
                    .hasPermission(user, university, permissionName);
        }
        
        // Fallback to original implementation
        Optional<UniversityMembership> membership = universityMembershipRepository
                .findByUserAndUniversity(user, university);
        
        return membership.isPresent() && 
               membership.get().isActive() && 
               hasPermissionInRole(permissionName, membership.get().getRole());
    }

    private boolean hasCommunityPermission(User user, String permissionName, Community community) {
        // Use optimized repository method if available
        if (communityMembershipRepository instanceof EnhancedCommunityMembershipRepository) {
            return ((EnhancedCommunityMembershipRepository) communityMembershipRepository)
                    .hasPermission(user, community, permissionName);
        }
        
        // Fallback to original implementation
        Optional<CommunityMembership> membership = communityMembershipRepository
                .findByUserAndCommunity(user, community);
        
        boolean hasCommunityPermission = membership.isPresent() && 
                membership.get().isActive() && 
                hasPermissionInRole(permissionName, membership.get().getRole());

        // Fall back to university permissions if community permission not found
        if (!hasCommunityPermission && community.getUniversity() != null) {
            return hasUniversityPermission(user, permissionName, community.getUniversity());
        }

        return hasCommunityPermission;
    }

    private boolean hasConversationPermission(User user, String permissionName, Conversation conversation) {
        // Use optimized repository method if available
        if (conversationParticipantRepository instanceof EnhancedConversationParticipantRepository) {
            return ((EnhancedConversationParticipantRepository) conversationParticipantRepository)
                    .hasPermission(user, conversation, permissionName);
        }
        
        // Fallback to original implementation
        Optional<ConversationParticipant> participant = conversationParticipantRepository
                .findByUserAndConversation(user, conversation);
        
        return participant.isPresent() && 
               participant.get().isActive() && 
               hasPermissionInRole(permissionName, participant.get().getRole());
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