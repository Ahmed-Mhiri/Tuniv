package com.tuniv.backend.community.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tuniv.backend.community.dto.CommunityCreateRequest;
import com.tuniv.backend.community.dto.CommunityDetailDto;
import com.tuniv.backend.community.dto.CommunitySummaryDto;
import com.tuniv.backend.community.service.CommunityService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.QuestionSummaryDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/communities")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    /**
     * GET /api/v1/communities
     * Get a paginated list of all communities, with optional search and university filter.
     */
    @GetMapping
    public ResponseEntity<Page<CommunitySummaryDto>> getAllCommunities(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer universityId,
            Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Page<CommunitySummaryDto> communities = communityService.getAllCommunities(search, universityId, pageable, currentUser);
        return ResponseEntity.ok(communities);
    }

    /**
     * GET /api/v1/communities/top
     * Get a list of the most popular communities.
     */
    @GetMapping("/top")
    public ResponseEntity<List<CommunitySummaryDto>> getTopCommunities(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<CommunitySummaryDto> communities = communityService.getTopCommunities(currentUser);
        return ResponseEntity.ok(communities);
    }

    /**
     * GET /api/v1/communities/joined
     * Get a list of communities the current user has joined.
     */
    @GetMapping("/joined")
    public ResponseEntity<List<CommunitySummaryDto>> getJoinedCommunities(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<CommunitySummaryDto> communities = communityService.getJoinedCommunities(currentUser);
        return ResponseEntity.ok(communities);
    }

    /**
     * GET /api/v1/communities/global
     * Get a list of global communities (not tied to any university).
     */
    @GetMapping("/global")
    public ResponseEntity<List<CommunitySummaryDto>> getGlobalCommunities(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<CommunitySummaryDto> communities = communityService.getGlobalCommunities(currentUser);
        return ResponseEntity.ok(communities);
    }

    /**
     * POST /api/v1/communities
     * Create a new community.
     */
    @PostMapping
    public ResponseEntity<CommunityDetailDto> createCommunity(
            @RequestBody @Valid CommunityCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        CommunityDetailDto createdCommunity = communityService.createCommunity(request, currentUser);
        return ResponseEntity.ok(createdCommunity);
    }

    /**
     * GET /api/v1/communities/{communityId}
     * Get detailed information about a specific community.
     */
    @GetMapping("/{communityId}")
    public ResponseEntity<CommunityDetailDto> getCommunityDetails(
            @PathVariable Integer communityId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        CommunityDetailDto communityDetails = communityService.getCommunityDetails(communityId, currentUser);
        return ResponseEntity.ok(communityDetails);
    }

    /**
     * POST /api/v1/communities/{communityId}/members
     * Join a specific community.
     */
    @PostMapping("/{communityId}/members")
    public ResponseEntity<?> joinCommunity(
            @PathVariable Integer communityId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        communityService.joinCommunity(communityId, currentUser);
        return ResponseEntity.ok(Map.of("message", "Successfully joined community."));
    }

    /**
     * DELETE /api/v1/communities/{communityId}/members
     * Leave a specific community.
     */
    @DeleteMapping("/{communityId}/members")
    public ResponseEntity<?> leaveCommunity(
            @PathVariable Integer communityId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        communityService.leaveCommunity(communityId, currentUser);
        return ResponseEntity.ok(Map.of("message", "Successfully left community."));
    }

    /**
     * GET /api/v1/communities/{communityId}/questions
     * Get a paginated list of questions for a specific community.
     */
    @GetMapping("/{communityId}/questions")
    public ResponseEntity<Page<QuestionSummaryDto>> getQuestionsByCommunity(
            @PathVariable Integer communityId,
            Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Page<QuestionSummaryDto> questions = communityService.getQuestionsByCommunity(communityId, pageable, currentUser);
        return ResponseEntity.ok(questions);
    }
}