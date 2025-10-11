package com.tuniv.backend.config;

import com.tuniv.backend.authorization.model.*;
import com.tuniv.backend.authorization.repository.PermissionRepository;
import com.tuniv.backend.chat.model.ConversationRole;
import com.tuniv.backend.chat.model.DefaultConversationRoles;
import com.tuniv.backend.chat.repository.ConversationRoleRepository;
import com.tuniv.backend.community.model.CommunityRole;
import com.tuniv.backend.community.model.DefaultCommunityRoles;
import com.tuniv.backend.community.repository.CommunityRoleRepository;
import com.tuniv.backend.university.model.UniversityRole;
import com.tuniv.backend.university.repository.UniversityRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final UniversityRoleRepository universityRoleRepository;
    private final CommunityRoleRepository communityRoleRepository;
    private final ConversationRoleRepository conversationRoleRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting data seeding...");
        
        try {
            seedPermissions();
            seedUniversityRoles();
            seedDefaultCommunityRoles();
            seedDefaultConversationRoles();
            
            log.info("Data seeding completed successfully");
        } catch (Exception e) {
            log.error("Data seeding failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void seedPermissions() {
        log.info("Seeding permissions...");
        
        // Seed Platform Permissions
        for (PlatformPermissions perm : PlatformPermissions.values()) {
            createPermissionIfNotFound(perm.getName(), perm.getDescription(), Permission.PermissionScope.PLATFORM);
        }

        // Seed University Permissions
        for (UniversityPermissions perm : UniversityPermissions.values()) {
            createPermissionIfNotFound(perm.getName(), perm.getDescription(), Permission.PermissionScope.UNIVERSITY);
        }

        // Seed Community Permissions
        for (CommunityPermissions perm : CommunityPermissions.values()) {
            createPermissionIfNotFound(perm.getName(), perm.getDescription(), Permission.PermissionScope.COMMUNITY);
        }

        // Seed Chat Permissions
        for (ChatPermissions perm : ChatPermissions.values()) {
            createPermissionIfNotFound(perm.getName(), perm.getDescription(), Permission.PermissionScope.CHAT);
        }

        // Seed Content Permissions (can be used in any scope)
        for (ContentPermissions perm : ContentPermissions.values()) {
            createPermissionIfNotFound(perm.getName(), perm.getDescription(), Permission.PermissionScope.UNIVERSITY);
        }
        
        log.info("Permissions seeding completed");
    }

    private void seedUniversityRoles() {
        log.info("Seeding university roles...");

        // University Admin (hierarchyLevel: 100)
        if (universityRoleRepository.findByName("university_admin").isEmpty()) {
            try {
                UniversityRole adminRole = new UniversityRole();
                adminRole.setId(1);
                adminRole.setName("university_admin");
                adminRole.setDisplayName("University Admin");
                adminRole.setHierarchyLevel(100);
                adminRole.setSystemRole(true);
                
                Set<Permission> adminPermissions = permissionRepository.findAll().stream()
                        .filter(perm -> perm.getScope() == Permission.PermissionScope.UNIVERSITY ||
                                       perm.getScope() == Permission.PermissionScope.PLATFORM)
                        .collect(Collectors.toSet());
                adminPermissions.addAll(getContentPermissions(true, true));
                adminRole.setPermissions(adminPermissions);
                
                universityRoleRepository.save(adminRole);
                log.info("Created University Admin role");
            } catch (Exception e) {
                log.error("Failed to create University Admin role: {}", e.getMessage());
                throw e;
            }
        }

        // University Moderator (hierarchyLevel: 80)
        if (universityRoleRepository.findByName("university_moderator").isEmpty()) {
            try {
                UniversityRole modRole = new UniversityRole();
                modRole.setId(2);
                modRole.setName("university_moderator");
                modRole.setDisplayName("University Moderator");
                modRole.setHierarchyLevel(80);
                modRole.setSystemRole(true);
                
                Set<String> modPermissionNames = Set.of(
                    UniversityPermissions.UNIVERSITY_USER_MANAGE.getName(),
                    UniversityPermissions.UNIVERSITY_REPORT_MANAGE.getName(),
                    ContentPermissions.POST_EDIT_ANY.getName(),
                    ContentPermissions.POST_DELETE_ANY.getName()
                );
                
                Set<Permission> modPermissions = permissionRepository.findByNameIn(modPermissionNames);
                modPermissions.addAll(getBasicContentPermissions());
                modRole.setPermissions(modPermissions);
                
                universityRoleRepository.save(modRole);
                log.info("Created University Moderator role");
            } catch (Exception e) {
                log.error("Failed to create University Moderator role: {}", e.getMessage());
                throw e;
            }
        }

        // Verified Student (hierarchyLevel: 20)
        if (universityRoleRepository.findByName("verified_student").isEmpty()) {
            try {
                UniversityRole studentRole = new UniversityRole();
                studentRole.setId(3);
                studentRole.setName("verified_student");
                studentRole.setDisplayName("Verified Student");
                studentRole.setHierarchyLevel(20);
                studentRole.setSystemRole(true);
                
                studentRole.setPermissions(getBasicContentPermissions());
                
                universityRoleRepository.save(studentRole);
                log.info("Created Verified Student role");
            } catch (Exception e) {
                log.error("Failed to create Verified Student role: {}", e.getMessage());
                throw e;
            }
        }

        // Unverified User (hierarchyLevel: 10)
        if (universityRoleRepository.findByName("unverified_user").isEmpty()) {
            try {
                UniversityRole unverifiedRole = new UniversityRole();
                unverifiedRole.setId(4);
                unverifiedRole.setName("unverified_user");
                unverifiedRole.setDisplayName("Unverified User");
                unverifiedRole.setHierarchyLevel(10);
                unverifiedRole.setSystemRole(true);
                
                // Limited permissions - mostly read-only
                Set<Permission> limitedPermissions = new HashSet<>();
                permissionRepository.findByName(ContentPermissions.POST_VOTE_CAST.getName())
                    .ifPresent(limitedPermissions::add);
                unverifiedRole.setPermissions(limitedPermissions);
                
                universityRoleRepository.save(unverifiedRole);
                log.info("Created Unverified User role");
            } catch (Exception e) {
                log.error("Failed to create Unverified User role: {}", e.getMessage());
                throw e;
            }
        }
        
        log.info("University roles seeding completed");
    }

    private void seedDefaultCommunityRoles() {
        log.info("Seeding default community roles...");

        // Community Owner/Admin
        if (communityRoleRepository.findByNameAndCommunityIsNull(DefaultCommunityRoles.COMMUNITY_OWNER.getName()).isEmpty()) {
            try {
                CommunityRole ownerRole = new CommunityRole();
                ownerRole.setName(DefaultCommunityRoles.COMMUNITY_OWNER.getName());
                ownerRole.setDisplayName(DefaultCommunityRoles.COMMUNITY_OWNER.getDisplayName());
                ownerRole.setSystemRole(true);
                ownerRole.setDefault(false);

                Set<Permission> ownerPermissions = permissionRepository.findAll().stream()
                        .filter(perm -> perm.getScope() == Permission.PermissionScope.COMMUNITY)
                        .collect(Collectors.toSet());
                ownerPermissions.addAll(getContentPermissions(true, true));
                ownerRole.setPermissions(ownerPermissions);

                communityRoleRepository.save(ownerRole);
                log.info("Created Community Owner role");
            } catch (Exception e) {
                log.error("Failed to create Community Owner role: {}", e.getMessage());
                throw e;
            }
        }

        // Community Moderator
        if (communityRoleRepository.findByNameAndCommunityIsNull(DefaultCommunityRoles.COMMUNITY_MODERATOR.getName()).isEmpty()) {
            try {
                CommunityRole modRole = new CommunityRole();
                modRole.setName(DefaultCommunityRoles.COMMUNITY_MODERATOR.getName());
                modRole.setDisplayName(DefaultCommunityRoles.COMMUNITY_MODERATOR.getDisplayName());
                modRole.setSystemRole(true);
                modRole.setDefault(false);

                Set<String> modPermissionNames = Set.of(
                    CommunityPermissions.COMMUNITY_MEMBER_KICK.getName(),
                    CommunityPermissions.COMMUNITY_TOPIC_PIN.getName(),
                    CommunityPermissions.COMMUNITY_TOPIC_LOCK.getName(),
                    CommunityPermissions.COMMUNITY_REPORT_MANAGE.getName(),
                    ContentPermissions.POST_EDIT_ANY.getName(),
                    ContentPermissions.POST_DELETE_ANY.getName()
                );

                Set<Permission> modPermissions = permissionRepository.findByNameIn(modPermissionNames);
                modPermissions.addAll(getBasicContentPermissions());
                modRole.setPermissions(modPermissions);

                communityRoleRepository.save(modRole);
                log.info("Created Community Moderator role");
            } catch (Exception e) {
                log.error("Failed to create Community Moderator role: {}", e.getMessage());
                throw e;
            }
        }

        // Community Member (default role)
        if (communityRoleRepository.findByNameAndCommunityIsNull(DefaultCommunityRoles.COMMUNITY_MEMBER.getName()).isEmpty()) {
            try {
                CommunityRole memberRole = new CommunityRole();
                memberRole.setName(DefaultCommunityRoles.COMMUNITY_MEMBER.getName());
                memberRole.setDisplayName(DefaultCommunityRoles.COMMUNITY_MEMBER.getDisplayName());
                memberRole.setSystemRole(true);
                memberRole.setDefault(true);

                memberRole.setPermissions(getBasicContentPermissions());

                communityRoleRepository.save(memberRole);
                log.info("Created Community Member role");
            } catch (Exception e) {
                log.error("Failed to create Community Member role: {}", e.getMessage());
                throw e;
            }
        }
        
        log.info("Community roles seeding completed");
    }

    private void seedDefaultConversationRoles() {
        log.info("Seeding default conversation roles...");

        // Conversation Admin
        if (conversationRoleRepository.findByNameAndConversationIsNull(DefaultConversationRoles.CONVERSATION_ADMIN.getName()).isEmpty()) {
            try {
                ConversationRole adminRole = new ConversationRole();
                adminRole.setName(DefaultConversationRoles.CONVERSATION_ADMIN.getName());
                adminRole.setDisplayName(DefaultConversationRoles.CONVERSATION_ADMIN.getDisplayName());
                adminRole.setSystemRole(true);
                adminRole.setDefault(false);

                Set<Permission> adminPermissions = permissionRepository.findAll().stream()
                        .filter(perm -> perm.getScope() == Permission.PermissionScope.CHAT)
                        .collect(Collectors.toSet());
                adminRole.setPermissions(adminPermissions);

                conversationRoleRepository.save(adminRole);
                log.info("Created Conversation Admin role");
            } catch (Exception e) {
                log.error("Failed to create Conversation Admin role: {}", e.getMessage());
                throw e;
            }
        }

        // Conversation Moderator
        if (conversationRoleRepository.findByNameAndConversationIsNull(DefaultConversationRoles.CONVERSATION_MODERATOR.getName()).isEmpty()) {
            try {
                ConversationRole modRole = new ConversationRole();
                modRole.setName(DefaultConversationRoles.CONVERSATION_MODERATOR.getName());
                modRole.setDisplayName(DefaultConversationRoles.CONVERSATION_MODERATOR.getDisplayName());
                modRole.setSystemRole(true);
                modRole.setDefault(false);

                Set<String> modPermissionNames = Set.of(
                    ChatPermissions.SEND_MESSAGES.getName(),
                    ChatPermissions.DELETE_OWN_MESSAGES.getName(),
                    ChatPermissions.EDIT_OWN_MESSAGES.getName(),
                    ChatPermissions.MESSAGE_REACT.getName(),
                    ChatPermissions.ADD_PARTICIPANTS.getName(),
                    ChatPermissions.REMOVE_PARTICIPANTS.getName(),
                    ChatPermissions.PIN_MESSAGES.getName(),
                    ChatPermissions.MUTE_PARTICIPANTS.getName()
                );

                Set<Permission> modPermissions = permissionRepository.findByNameIn(modPermissionNames);
                modRole.setPermissions(modPermissions);

                conversationRoleRepository.save(modRole);
                log.info("Created Conversation Moderator role");
            } catch (Exception e) {
                log.error("Failed to create Conversation Moderator role: {}", e.getMessage());
                throw e;
            }
        }

        // Conversation Member (default role)
        if (conversationRoleRepository.findByNameAndConversationIsNull(DefaultConversationRoles.CONVERSATION_MEMBER.getName()).isEmpty()) {
            try {
                ConversationRole memberRole = new ConversationRole();
                memberRole.setName(DefaultConversationRoles.CONVERSATION_MEMBER.getName());
                memberRole.setDisplayName(DefaultConversationRoles.CONVERSATION_MEMBER.getDisplayName());
                memberRole.setSystemRole(true);
                memberRole.setDefault(true);

                Set<String> memberPermissionNames = Set.of(
                    ChatPermissions.SEND_MESSAGES.getName(),
                    ChatPermissions.DELETE_OWN_MESSAGES.getName(),
                    ChatPermissions.EDIT_OWN_MESSAGES.getName(),
                    ChatPermissions.MESSAGE_REACT.getName()
                );

                Set<Permission> memberPermissions = permissionRepository.findByNameIn(memberPermissionNames);
                memberRole.setPermissions(memberPermissions);

                conversationRoleRepository.save(memberRole);
                log.info("Created Conversation Member role");
            } catch (Exception e) {
                log.error("Failed to create Conversation Member role: {}", e.getMessage());
                throw e;
            }
        }
        
        log.info("Conversation roles seeding completed");
    }

    private void createPermissionIfNotFound(String name, String description, Permission.PermissionScope scope) {
        try {
            if (permissionRepository.findByName(name).isEmpty()) {
                Permission permission = new Permission();
                permission.setName(name);
                permission.setDescription(description);
                permission.setScope(scope);
                permissionRepository.save(permission);
                log.debug("Created permission: {}", name);
            }
        } catch (Exception e) {
            log.error("Failed to create permission {}: {}", name, e.getMessage());
            throw new RuntimeException("Permission creation failed: " + name, e);
        }
    }

    private Set<Permission> getBasicContentPermissions() {
        Set<String> basicPermissionNames = Set.of(
            ContentPermissions.POST_TOPIC_CREATE.getName(),
            ContentPermissions.POST_REPLY_CREATE.getName(),
            ContentPermissions.POST_EDIT_OWN.getName(),
            ContentPermissions.POST_DELETE_OWN.getName(),
            ContentPermissions.POST_VOTE_CAST.getName(),
            ContentPermissions.MESSAGE_SEND.getName(),
            ContentPermissions.MESSAGE_DELETE_OWN.getName(),
            ContentPermissions.MESSAGE_REACT.getName()
        );
        
        Set<Permission> permissions = permissionRepository.findByNameIn(basicPermissionNames);
        
        // Log any missing permissions for debugging
        Set<String> foundPermissions = permissions.stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());
        
        Set<String> missingPermissions = new HashSet<>(basicPermissionNames);
        missingPermissions.removeAll(foundPermissions);
        
        if (!missingPermissions.isEmpty()) {
            log.warn("Missing basic content permissions: {}", missingPermissions);
        }
        
        return permissions;
    }

    private Set<Permission> getContentPermissions(boolean editAny, boolean deleteAny) {
        Set<Permission> permissions = new HashSet<>(getBasicContentPermissions());
        
        if (editAny) {
            permissionRepository.findByName(ContentPermissions.POST_EDIT_ANY.getName())
                    .ifPresentOrElse(
                        permissions::add,
                        () -> log.warn("Permission not found: {}", ContentPermissions.POST_EDIT_ANY.getName())
                    );
        }
        
        if (deleteAny) {
            permissionRepository.findByName(ContentPermissions.POST_DELETE_ANY.getName())
                    .ifPresentOrElse(
                        permissions::add,
                        () -> log.warn("Permission not found: {}", ContentPermissions.POST_DELETE_ANY.getName())
                    );
            permissionRepository.findByName(ContentPermissions.MESSAGE_DELETE_ANY.getName())
                    .ifPresentOrElse(
                        permissions::add,
                        () -> log.warn("Permission not found: {}", ContentPermissions.MESSAGE_DELETE_ANY.getName())
                    );
        }
        
        return permissions;
    }
}