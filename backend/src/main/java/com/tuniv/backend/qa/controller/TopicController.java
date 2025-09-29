package com.tuniv.backend.qa.controller;

import java.util.List;
import java.util.Map;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.ReplyCreateRequest;
import com.tuniv.backend.qa.dto.TopicCreateRequest;
import com.tuniv.backend.qa.dto.TopicResponseDto;
import com.tuniv.backend.qa.dto.TopicSummaryDto;
import com.tuniv.backend.qa.dto.TopicUpdateRequest;
import com.tuniv.backend.qa.dto.VoteRequest;
import com.tuniv.backend.qa.service.TopicService;
import com.tuniv.backend.qa.service.VoteService;
import com.tuniv.backend.shared.service.RateLimitingService;

import io.github.bucket4j.Bucket;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;
    // Assuming these services exist to preserve voting/rate-limiting functionality
    private final VoteService voteService;
    private final RateLimitingService rateLimitingService;
    private final CacheManager cacheManager;

    // A simple record for the solution request body
    public record MarkSolutionRequest(Integer replyId) {}

    /**
     * GET /api/v1/topics
     * Gets a paginated list of topics for a given module OR community.
     * Exactly one of moduleId or communityId must be provided.
     */
    @GetMapping
    public ResponseEntity<Page<TopicSummaryDto>> getTopics(
            @RequestParam(required = false) Integer moduleId,
            @RequestParam(required = false) Integer communityId,
            Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        if ((moduleId == null && communityId == null) || (moduleId != null && communityId != null)) {
            throw new IllegalArgumentException("You must provide exactly one of 'moduleId' or 'communityId'.");
        }

        Page<TopicSummaryDto> topics;
        if (moduleId != null) {
            topics = topicService.getTopicsByModule(moduleId, pageable, currentUser);
        } else {
            topics = topicService.getTopicsByCommunity(communityId, pageable, currentUser);
        }
        return ResponseEntity.ok(topics);
    }

    /**
     * POST /api/v1/topics
     * Creates a new topic (either a QUESTION or a POST).
     */
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<TopicResponseDto> createTopic(
            @RequestPart("topic") @Valid TopicCreateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        TopicResponseDto newTopic = topicService.createTopic(request, currentUser, files);
        return new ResponseEntity<>(newTopic, HttpStatus.CREATED);
    }

    /**
     * GET /api/v1/topics/{topicId}
     * Retrieves a single topic with its full reply tree.
     */
    @GetMapping("/{topicId}")
    public ResponseEntity<TopicResponseDto> getTopicById(
            @PathVariable Integer topicId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        TopicResponseDto topic = topicService.getTopicById(topicId, currentUser);
        return ResponseEntity.ok(topic);
    }

    /**
     * PUT /api/v1/topics/{topicId}
     * Updates an existing topic's title, body, tags, and attachments.
     */
    @PutMapping(value = "/{topicId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<TopicResponseDto> updateTopic(
            @PathVariable Integer topicId,
            @RequestPart("topic") @Valid TopicUpdateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> newFiles,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        TopicResponseDto updatedTopic = topicService.updateTopic(topicId, request, newFiles, currentUser);
        return ResponseEntity.ok(updatedTopic);
    }

    /**
     * DELETE /api/v1/topics/{topicId}
     * Deletes a topic. Requires ownership.
     */
    @DeleteMapping("/{topicId}")
    public ResponseEntity<Void> deleteTopic(
            @PathVariable Integer topicId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        topicService.deleteTopic(topicId, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/topics/{topicId}/replies
     * Adds a reply (or a nested comment) to a topic.
     */
    @PostMapping(value = "/{topicId}/replies", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<TopicResponseDto> addReply(
            @PathVariable Integer topicId,
            @RequestPart("reply") @Valid ReplyCreateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        TopicResponseDto updatedTopic = topicService.addReply(topicId, request, currentUser, files);
        return new ResponseEntity<>(updatedTopic, HttpStatus.CREATED);
    }

    /**
     * POST /api/v1/topics/{topicId}/solution
     * Marks a specific reply as the accepted solution for a topic.
     */
    @PostMapping("/{topicId}/solution")
    public ResponseEntity<TopicResponseDto> markAsSolution(
            @PathVariable Integer topicId,
            @RequestBody @Valid MarkSolutionRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        topicService.markAsSolution(topicId, request.replyId(), currentUser);
        TopicResponseDto updatedTopic = topicService.getTopicById(topicId, currentUser);
        return ResponseEntity.ok(updatedTopic);
    }

    /**
     * DELETE /api/v1/topics/{topicId}/solution
     * Unmarks the accepted solution for a topic.
     */
    @DeleteMapping("/{topicId}/solution")
    public ResponseEntity<TopicResponseDto> unmarkSolution(
            @PathVariable Integer topicId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        topicService.unmarkSolution(topicId, currentUser);
        TopicResponseDto updatedTopic = topicService.getTopicById(topicId, currentUser);
        return ResponseEntity.ok(updatedTopic);
    }
    
    /**
     * POST /api/v1/topics/{topicId}/vote
     * Casts a vote (up or down) on a topic.
     */
    @PostMapping("/{topicId}/vote")
    public ResponseEntity<?> voteOnTopic(
            @PathVariable Integer topicId,
            @RequestBody @Valid VoteRequest voteRequest,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        // Assuming rate limiting logic is preserved
        Bucket bucket = rateLimitingService.resolveBucket(currentUser.getId().toString());
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Rate limit for voting exceeded."));
        }

        voteService.voteOnTopic(topicId, currentUser, voteRequest.value());

        
        // Evict from cache and return the fresh state
        evictTopicFromCache(topicId);
        return ResponseEntity.ok(topicService.getTopicById(topicId, currentUser));
    }

    /**
     * POST /api/v1/topics/{topicId}/replies/{replyId}/vote
     * Casts a vote (up or down) on a reply.
     */
    @PostMapping("/{topicId}/replies/{replyId}/vote")
    public ResponseEntity<?> voteOnReply(
            @PathVariable Integer topicId,
            @PathVariable Integer replyId,
            @RequestBody @Valid VoteRequest voteRequest,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        Bucket bucket = rateLimitingService.resolveBucket(currentUser.getId().toString());
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Rate limit for voting exceeded."));
        }
        
        voteService.voteOnReply(replyId, currentUser, voteRequest.value());


        // Evict the parent topic from cache and return the fresh state
        evictTopicFromCache(topicId);
        return ResponseEntity.ok(topicService.getTopicById(topicId, currentUser));
    }

    /**
     * GET /api/v1/topics/user/{userId}/solved
     * Gets topics created by a user that are marked as solved.
     */
    @GetMapping("/user/{userId}/solved")
    public ResponseEntity<Page<TopicSummaryDto>> getSolvedTopicsByUser(
            @PathVariable Integer userId, Pageable pageable, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        Page<TopicSummaryDto> topics = topicService.getSolvedTopicsByUser(userId, pageable, currentUser);
        return ResponseEntity.ok(topics);
    }

    /**
     * GET /api/v1/topics/user/{userId}/solutions-provided
     * Gets topics where a user's reply was accepted as the solution.
     */
    @GetMapping("/user/{userId}/solutions-provided")
    public ResponseEntity<Page<TopicSummaryDto>> getTopicsWithUserSolutions(
            @PathVariable Integer userId, Pageable pageable, @AuthenticationPrincipal UserDetailsImpl currentUser) {
                
        Page<TopicSummaryDto> topics = topicService.getTopicsWithUserSolutions(userId, pageable, currentUser);
        return ResponseEntity.ok(topics);
    }

    private void evictTopicFromCache(Integer topicId) {
        Cache topicCache = cacheManager.getCache("topics");
        if (topicCache != null) {
            topicCache.evict(topicId);
        }
    }
}
