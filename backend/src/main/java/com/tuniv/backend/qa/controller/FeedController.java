package com.tuniv.backend.qa.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.TopicSummaryDto;
import com.tuniv.backend.qa.service.FeedService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    /**
     * GET /api/v1/feed
     * Retrieves the personalized feed for the currently authenticated user,
     * containing topics from followed users, communities, tags, and modules.
     * Requires authentication.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<TopicSummaryDto>> getPersonalizedFeed(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            Pageable pageable) {
        Page<TopicSummaryDto> feed = feedService.getPersonalizedFeed(currentUser, pageable);
        return ResponseEntity.ok(feed);
    }

    /**
     * GET /api/v1/feed/popular
     * Retrieves a feed of the most popular topics across the platform.
     * This endpoint is public.
     */
    @GetMapping("/popular")
    public ResponseEntity<Page<TopicSummaryDto>> getPopularFeed(
            Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Page<TopicSummaryDto> feed = feedService.getPopularFeed(pageable, currentUser);
        return ResponseEntity.ok(feed);
    }

    /**
     * GET /api/v1/feed/module/{moduleId}
     * Retrieves a feed of all topics within a specific module.
     */
    @GetMapping("/module/{moduleId}")
    public ResponseEntity<Page<TopicSummaryDto>> getModuleFeed(
            @PathVariable Integer moduleId,
            Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Page<TopicSummaryDto> feed = feedService.getTopicsByModule(moduleId, pageable, currentUser);
        return ResponseEntity.ok(feed);
    }

    /**
     * GET /api/v1/feed/community/{communityId}
     * Retrieves a feed of all topics within a specific community.
     */
    @GetMapping("/community/{communityId}")
    public ResponseEntity<Page<TopicSummaryDto>> getCommunityFeed(
            @PathVariable Integer communityId,
            Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Page<TopicSummaryDto> feed = feedService.getTopicsByCommunity(communityId, pageable, currentUser);
        return ResponseEntity.ok(feed);
    }

    /**
     * GET /api/v1/feed/tag/{tagName}
     * Retrieves a feed of all topics associated with a specific tag.
     */
    @GetMapping("/tag/{tagName}")
    public ResponseEntity<Page<TopicSummaryDto>> getTagFeed(
            @PathVariable String tagName,
            Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Page<TopicSummaryDto> feed = feedService.getTopicsByTag(tagName, pageable, currentUser);
        return ResponseEntity.ok(feed);
    }
}