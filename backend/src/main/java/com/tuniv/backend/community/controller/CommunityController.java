package com.tuniv.backend.community.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tuniv.backend.community.dto.CommunityCreateRequest;
import com.tuniv.backend.community.dto.CommunityDetailDto;
import com.tuniv.backend.community.dto.CommunityStatsDto;
import com.tuniv.backend.community.dto.CommunitySummaryDto;
import com.tuniv.backend.community.dto.CommunityUpdateRequest;
import com.tuniv.backend.community.dto.CommunityWithStatsDto;
import com.tuniv.backend.community.dto.TrendingCommunityDto;
import com.tuniv.backend.community.service.CommunityService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.TopicSummaryDto;
import com.tuniv.backend.qa.model.TopicType;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/communities")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    /**
     * POST /api/v1/communities
     * Creates a new community.
     * The creator automatically becomes a moderator.
     */
    @PostMapping
    public ResponseEntity<CommunityDetailDto> createCommunity(
            @RequestBody @Valid CommunityCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        CommunityDetailDto createdCommunity = communityService.createCommunity(request, currentUser);
        return ResponseEntity.ok(createdCommunity);
    }

    /**
     * PATCH /api/v1/communities/{communityId}
     * Updates a community's information. Only moderators can perform this action.
     */
    @PatchMapping("/{communityId}")
    public ResponseEntity<CommunityDetailDto> updateCommunity(
            @PathVariable Integer communityId,
            @RequestBody @Valid CommunityUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        CommunityDetailDto updatedCommunity = communityService.updateCommunity(communityId, request, currentUser);
        return ResponseEntity.ok(updatedCommunity);
    }
    
    /**
     * GET /api/v1/communities/search
     * Provides an advanced search for communities with multiple filters.
     * Replaces the previous simple GET /.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<CommunitySummaryDto>> searchCommunities(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer minTopics,
            @RequestParam(required = false) Integer minMembers,
            Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Page<CommunitySummaryDto> communities = communityService.searchCommunitiesWithFilters(
            search, minTopics, minMembers, pageable, currentUser
        );
        return ResponseEntity.ok(communities);
    }

    /**
     * GET /api/v1/communities/trending
     * Gets a list of communities with the most activity in the last week.
     */
    @GetMapping("/trending")
    public ResponseEntity<List<TrendingCommunityDto>> getTrendingCommunities(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<TrendingCommunityDto> communities = communityService.getTrendingCommunities(currentUser);
        return ResponseEntity.ok(communities);
    }

    /**
     * GET /api/v1/communities/popular
     * Gets a list of popular communities, optionally filtered by a minimum number of members.
     */
    @GetMapping("/popular")
    public ResponseEntity<List<CommunitySummaryDto>> getPopularCommunities(
            @RequestParam(required = false, defaultValue = "10") Integer minMembers,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<CommunitySummaryDto> communities = communityService.getPopularCommunities(minMembers, currentUser);
        return ResponseEntity.ok(communities);
    }
    
    /**
     * GET /api/v1/communities/active
     * Gets a list of communities with recent topic activity (within the last 24 hours).
     */
    @GetMapping("/active")
    public ResponseEntity<List<CommunitySummaryDto>> getActiveCommunities(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<CommunitySummaryDto> communities = communityService.getActiveCommunities(currentUser);
        return ResponseEntity.ok(communities);
    }

    /**
     * GET /api/v1/communities/joined
     * Gets a list of communities the current user has joined.
     */
    @GetMapping("/joined")
    public ResponseEntity<List<CommunitySummaryDto>> getJoinedCommunities(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<CommunitySummaryDto> communities = communityService.getJoinedCommunities(currentUser);
        return ResponseEntity.ok(communities);
    }
    
    /**
     * GET /api/v1/communities/university/{universityId}
     * Gets communities associated with a specific university, ordered by topic count.
     */
    @GetMapping("/university/{universityId}")
    public ResponseEntity<List<CommunitySummaryDto>> getCommunitiesByUniversity(
            @PathVariable Integer universityId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<CommunitySummaryDto> communities = communityService.getCommunitiesByUniversity(universityId, currentUser);
        return ResponseEntity.ok(communities);
    }

    /**
     * GET /api/v1/communities/{communityId}
     * Gets detailed information and statistics for a specific community.
     */
    @GetMapping("/{communityId}")
    public ResponseEntity<CommunityWithStatsDto> getCommunityWithStats(
            @PathVariable Integer communityId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        CommunityWithStatsDto communityDetails = communityService.getCommunityWithStats(communityId, currentUser);
        return ResponseEntity.ok(communityDetails);
    }

    /**
     * POST /api/v1/communities/{communityId}/join
     * Allows the current user to join a specific community.
     */
    @PostMapping("/{communityId}/join")
    public ResponseEntity<?> joinCommunity(
            @PathVariable Integer communityId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        communityService.joinCommunity(communityId, currentUser);
        return ResponseEntity.ok(Map.of("message", "Successfully joined community."));
    }

    /**
     * DELETE /api/v1/communities/{communityId}/leave
     * Allows the current user to leave a specific community.
     */
    @DeleteMapping("/{communityId}/leave")
    public ResponseEntity<?> leaveCommunity(
            @PathVariable Integer communityId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        communityService.leaveCommunity(communityId, currentUser);
        return ResponseEntity.ok(Map.of("message", "Successfully left community."));
    }

    /**
     * GET /api/v1/communities/{communityId}/topics
     * Gets a paginated list of topics for a community, with an optional filter for topic type.
     */
    @GetMapping("/{communityId}/topics")
    public ResponseEntity<Page<TopicSummaryDto>> getTopicsByCommunity(
            @PathVariable Integer communityId,
            @RequestParam(required = false) TopicType type,
            Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        Page<TopicSummaryDto> topics;
        if (type != null) {
            topics = communityService.getTopicsByCommunityAndType(communityId, type, pageable, currentUser);
        } else {
            topics = communityService.getTopicsByCommunity(communityId, pageable, currentUser);
        }
        return ResponseEntity.ok(topics);
    }

    /**
     * GET /api/v1/communities/{communityId}/stats
     * Retrieves key statistics for a community.
     */
    @GetMapping("/{communityId}/stats")
    public ResponseEntity<CommunityStatsDto> getCommunityStats(@PathVariable Integer communityId) {
        CommunityStatsDto stats = communityService.getCommunityStats(communityId);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * GET /api/v1/communities/{communityId}/stats/topic-distribution
     * Gets the distribution of topic types (QUESTION, POST) within a community.
     */
    @GetMapping("/{communityId}/stats/topic-distribution")
    public ResponseEntity<Map<TopicType, Long>> getTopicDistribution(@PathVariable Integer communityId) {
        Map<TopicType, Long> distribution = communityService.getCommunityTopicDistribution(communityId);
        return ResponseEntity.ok(distribution);
    }
    
    /**
     * GET /api/v1/communities/{communityId}/access
     * Checks if a user can access a specific community.
     */
    @GetMapping("/{communityId}/access")
    public ResponseEntity<Map<String, Boolean>> canAccessCommunity(
            @PathVariable Integer communityId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        boolean canAccess = communityService.canAccessCommunity(communityId, currentUser);
        return ResponseEntity.ok(Map.of("canAccess", canAccess));
    }
}