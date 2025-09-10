package com.tuniv.backend.qa.controller;

import java.util.List;
import java.util.Map;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping; // <-- IMPORT ADDED
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.config.security.services.UserDetailsImpl; // <-- IMPORT ADDED
import com.tuniv.backend.qa.dto.CommentCreateRequest;
import com.tuniv.backend.qa.dto.CommentUpdateRequest;
import com.tuniv.backend.qa.dto.QuestionResponseDto;
import com.tuniv.backend.qa.dto.VoteRequest;
import com.tuniv.backend.qa.service.CommentService;
import com.tuniv.backend.qa.service.QuestionService;
import com.tuniv.backend.qa.service.VoteService;
import com.tuniv.backend.shared.service.RateLimitingService;

import io.github.bucket4j.Bucket;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/answers/{answerId}/comments") // Nested under Answers
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final QuestionService questionService;
    private final VoteService voteService;
    private final RateLimitingService rateLimitingService;
    private final CacheManager cacheManager; // Injected for manual cache eviction

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<QuestionResponseDto> createComment(
            @PathVariable Integer answerId, // answerId is from the path
            @RequestPart("comment") @Valid CommentCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        // The service method now gets the answerId directly
        commentService.createComment(answerId, request, currentUser, files);

        // After creating, return the entire updated question DTO
        Integer questionId = questionService.findQuestionIdByAnswerId(answerId);
        QuestionResponseDto updatedQuestion = questionService.getQuestionById(questionId, currentUser);
        return new ResponseEntity<>(updatedQuestion, HttpStatus.CREATED);
    }

    @PutMapping(value = "/{commentId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<QuestionResponseDto> updateComment(
            @PathVariable Integer answerId, // Included for RESTful context
            @PathVariable Integer commentId,
            @RequestPart("comment") @Valid CommentUpdateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> newFiles,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        commentService.updateComment(commentId, request, newFiles, currentUser);

        Integer questionId = questionService.findQuestionIdByCommentId(commentId);

        // Evict cache to ensure fresh data is loaded after an update
        Cache questionsCache = cacheManager.getCache("questions");
        if (questionsCache != null) {
            questionsCache.evict(questionId);
        }
        
        QuestionResponseDto updatedQuestion = questionService.getQuestionById(questionId, currentUser);
        return ResponseEntity.ok(updatedQuestion);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Integer answerId, // Included for RESTful context
            @PathVariable Integer commentId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        commentService.deleteComment(commentId, currentUser);
        // Deleting a child resource doesn't require returning the parent state,
        // as the frontend will trigger a full refresh via its own (deleted) output event.
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{commentId}/vote")
    public ResponseEntity<?> voteOnComment(
            @PathVariable Integer answerId, // Included for RESTful context
            @PathVariable Integer commentId,
            @Valid @RequestBody VoteRequest voteRequest,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        Bucket bucket = rateLimitingService.resolveBucket(currentUser.getId().toString());

        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "You have exhausted your vote limit. Please try again later."));
        }

        // 1. Perform the database update
        voteService.voteOnComment(commentId, currentUser, (short) voteRequest.value());

        // 2. Find the parent question's ID
        Integer questionId = questionService.findQuestionIdByCommentId(commentId);

        // 3. IMPORTANT: Evict the parent question from the cache to bust stale data
        Cache questionsCache = cacheManager.getCache("questions");
        if (questionsCache != null) {
            questionsCache.evict(questionId);
        }

        // 4. Fetch the fresh, complete Question object
        QuestionResponseDto updatedQuestion = questionService.getQuestionById(questionId, currentUser);
        
        // 5. Return it to the frontend so the UI can update
        return ResponseEntity.ok(updatedQuestion);
    }
}