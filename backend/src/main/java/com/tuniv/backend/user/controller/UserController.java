package com.tuniv.backend.user.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuniv.backend.community.dto.CommunitySummaryDto;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.user.dto.LeaderboardUserDto;
import com.tuniv.backend.user.dto.UserActivityItemDto;
import com.tuniv.backend.user.dto.UserProfileDto;
import com.tuniv.backend.user.dto.UserProfileUpdateRequest;
import com.tuniv.backend.user.service.ActivityService;
import com.tuniv.backend.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ActivityService activityService;
    // ❌ REMOVED: The old FollowService is no longer needed here.

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getCurrentUserProfile(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(userService.getCurrentUserProfile(currentUser));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDto> updateCurrentUserProfile(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @Valid @RequestBody UserProfileUpdateRequest updateRequest) {
        return ResponseEntity.ok(userService.updateCurrentUserProfile(currentUser, updateRequest));
    }

    @GetMapping("/me/communities")
    public ResponseEntity<List<CommunitySummaryDto>> getCurrentUserCommunities( // ✅ UPDATED return type
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        return ResponseEntity.ok(userService.getUserCommunities(currentUser));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardUserDto>> getLeaderboard() {
        return ResponseEntity.ok(userService.getLeaderboardUsers());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileDto> getUserProfileById(@PathVariable Integer userId) {
        return ResponseEntity.ok(userService.getUserProfileById(userId));
    }

    @GetMapping("/{id}/activity")
    public ResponseEntity<List<UserActivityItemDto>> getUserActivity(@PathVariable Integer id) {
        List<UserActivityItemDto> activity = activityService.getActivityForUser(id);
        return ResponseEntity.ok(activity);
    }

    // ❌ REMOVED: The user-specific follow/unfollow endpoints are gone.
    // This logic will be handled by a new, more generic FollowController.
}