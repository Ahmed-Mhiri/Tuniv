package com.tuniv.backend.community.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuniv.backend.community.dto.RoleCreateRequest;
import com.tuniv.backend.community.dto.RoleUpdateRequest;
import com.tuniv.backend.community.dto.UpdateMemberRoleRequest;
import com.tuniv.backend.community.model.CommunityRole;
import com.tuniv.backend.community.service.CommunityRoleService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/roles")
@RequiredArgsConstructor
public class CommunityRoleController {

    private final CommunityRoleService roleService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<CommunityRole> createRole(
            @PathVariable Integer communityId,
            @RequestBody @Valid RoleCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        User adminUser = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        CommunityRole role = roleService.createRole(communityId, request, adminUser);
        return ResponseEntity.ok(role);
    }

    @GetMapping
    public ResponseEntity<List<CommunityRole>> listRoles(@PathVariable Integer communityId) {
        List<CommunityRole> roles = roleService.listRoles(communityId);
        return ResponseEntity.ok(roles);
    }

    @PatchMapping("/{roleId}")
    public ResponseEntity<CommunityRole> updateRole(
            @PathVariable Integer communityId,
            @PathVariable Integer roleId,
            @RequestBody @Valid RoleUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        User adminUser = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        CommunityRole role = roleService.updateRole(roleId, request, adminUser);
        return ResponseEntity.ok(role);
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<Map<String, String>> deleteRole(
            @PathVariable Integer communityId,
            @PathVariable Integer roleId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        User adminUser = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        roleService.deleteRole(roleId, adminUser);
        return ResponseEntity.ok(Map.of("message", "Role deleted successfully"));
    }

    @PostMapping("/assign")
    public ResponseEntity<Map<String, String>> assignRoleToMember(
            @PathVariable Integer communityId,
            @RequestBody @Valid UpdateMemberRoleRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        User adminUser = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        roleService.assignRoleToMember(communityId, request, adminUser);
        return ResponseEntity.ok(Map.of("message", "Role assigned successfully"));
    }
}