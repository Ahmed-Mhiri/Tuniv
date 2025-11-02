package com.tuniv.backend.community.service;

import com.tuniv.backend.authorization.service.PermissionService;
import com.tuniv.backend.authorization.model.Permission;
import com.tuniv.backend.authorization.repository.PermissionRepository;
import com.tuniv.backend.community.dto.RoleUpdateRequest;
import com.tuniv.backend.community.dto.UpdateMemberRoleRequest;
import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.community.model.CommunityMembership;
import com.tuniv.backend.community.model.CommunityRole;
import com.tuniv.backend.community.repository.CommunityMembershipRepository;
import com.tuniv.backend.community.repository.CommunityRepository;
import com.tuniv.backend.community.repository.CommunityRoleRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;

import com.tuniv.backend.community.dto.RoleCreateRequest;

@Service
@RequiredArgsConstructor
public class CommunityRoleService {

    private final CommunityRoleRepository roleRepository;
    private final CommunityRepository communityRepository;
    private final PermissionRepository permissionRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    @Transactional
    public CommunityRole createRole(Integer communityId, RoleCreateRequest request, User adminUser) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

        // Check if adminUser has permission to manage roles using PermissionService
        validateAdminPermission(communityId, adminUser);

        // Check if role name already exists in this community
        if (roleRepository.findByNameAndIsSystemRole(request.name(), false).isPresent()) {
            throw new IllegalArgumentException("Role name already exists in this community");
        }

        CommunityRole role = new CommunityRole();
        role.setName(request.name());
        role.setDisplayName(request.displayName());
        role.setCommunity(community);
        role.setSystemRole(false);
        role.setDefault(false);

        // Set permissions
        Set<Permission> permissions = new HashSet<>();
        for (String permissionName : request.permissions()) {
            Permission permission = permissionRepository.findByName(permissionName)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionName));
            permissions.add(permission);
        }
        role.setPermissions(permissions);

        return roleRepository.save(role);
    }

    @Transactional
    public CommunityRole updateRole(Integer roleId, RoleUpdateRequest request, User adminUser) {
        CommunityRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        // Check if adminUser has permission to manage roles using PermissionService
        validateAdminPermission(role.getCommunity().getCommunityId(), adminUser);

        // Prevent modification of system roles
        if (role.isSystemRole()) {
            throw new IllegalArgumentException("Cannot modify system roles");
        }

        if (request.displayName() != null) {
            role.setDisplayName(request.displayName());
        }

        if (request.permissions() != null) {
            Set<Permission> permissions = new HashSet<>();
            for (String permissionName : request.permissions()) {
                Permission permission = permissionRepository.findByName(permissionName)
                        .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionName));
                permissions.add(permission);
            }
            role.setPermissions(permissions);
        }

        return roleRepository.save(role);
    }

    @Transactional
    public void deleteRole(Integer roleId, User adminUser) {
        CommunityRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        // Check if adminUser has permission to manage roles using PermissionService
        validateAdminPermission(role.getCommunity().getCommunityId(), adminUser);

        // Prevent deletion of system roles
        if (role.isSystemRole()) {
            throw new IllegalArgumentException("Cannot delete system roles");
        }

        // Check if any members still have this role
        List<CommunityMembership> membersWithRole = membershipRepository.findById_CommunityIdAndRole(
                role.getCommunity().getCommunityId(), role);

        if (!membersWithRole.isEmpty()) {
            throw new IllegalStateException("Cannot delete role that is still assigned to members");
        }

        roleRepository.delete(role);
    }

    @Transactional
    public void assignRoleToMember(Integer communityId, UpdateMemberRoleRequest request, User adminUser) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

        // Check if adminUser has permission to manage roles using PermissionService
        validateAdminPermission(communityId, adminUser);

        CommunityMembership membership = membershipRepository
                .findById_UserIdAndId_CommunityId(request.userId(), communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        CommunityRole newRole = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        // Ensure the role belongs to this community
        if (!newRole.getCommunity().getCommunityId().equals(communityId)) {
            throw new IllegalArgumentException("Role does not belong to this community");
        }

        membership.setRole(newRole);
        membershipRepository.save(membership);
    }

    @Transactional(readOnly = true)
    public List<CommunityRole> listRoles(Integer communityId) {
        return roleRepository.findByCommunity_CommunityId(communityId);
    }

    private void validateAdminPermission(Integer communityId, User adminUser) {
        // Fetch the community for the permission check context
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

        if (!permissionService.hasPermission(
                adminUser.getUserId(), 
                CommunityPermissions.COMMUNITY_ROLE_MANAGE.getName(), 
                community)) {
            throw new AccessDeniedException("Insufficient permissions to manage roles");
        }

        // The membership check is no longer needed here, 
        // as hasPermission() will handle non-members correctly (they'll have no roles/permissions).
    }
}