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

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.user.dto.CommunityDto;
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

    // ✅ RULE: Place the most specific endpoints FIRST.

    /**
     * Endpoint to get the current user's profile.
     * Specific path: /me
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getCurrentUserProfile(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(userService.getCurrentUserProfile(currentUser));
    }

    /**
     * Endpoint to update the current user's profile.
     */
    @PutMapping("/me")
    public ResponseEntity<UserProfileDto> updateCurrentUserProfile(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @Valid @RequestBody UserProfileUpdateRequest updateRequest) {
        return ResponseEntity.ok(userService.updateCurrentUserProfile(currentUser, updateRequest));
    }

    /**
     * Endpoint to get the current user's joined communities.
     * Specific path: /me/communities
     */
    @GetMapping("/me/communities")
    public ResponseEntity<List<CommunityDto>> getCurrentUserCommunities(
        @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        return ResponseEntity.ok(userService.getUserCommunities(currentUser));
    }

    /**
     * Endpoint to get the site-wide leaderboard.
     * Specific path: /leaderboard
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardUserDto>> getLeaderboard() {
        return ResponseEntity.ok(userService.getLeaderboardUsers());
    }

    // ✅ RULE: Place endpoints with path variables AFTER the specific ones.

    /**
     * Endpoint to get a specific user's profile by their ID.
     * General path: /{userId} - This should come after /leaderboard and /me.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileDto> getUserProfileById(@PathVariable Integer userId) {
        return ResponseEntity.ok(userService.getUserProfileById(userId));
    }

    /**
     * Endpoint to get a specific user's activity.
     * General path: /{id}/activity
     */
    @GetMapping("/{id}/activity")
    public ResponseEntity<List<UserActivityItemDto>> getUserActivity(@PathVariable Integer id) {
        List<UserActivityItemDto> activity = activityService.getActivityForUser(id);
        return ResponseEntity.ok(activity);
    }

    
}