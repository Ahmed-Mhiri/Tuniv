package com.tuniv.backend.qa.controller;

import java.util.List;
import java.util.Map;

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
@RequestMapping("/api/v1/answers/{answerId}/comments") // ✅ Nested under Answers
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final QuestionService questionService;
    private final VoteService voteService;
    private final RateLimitingService rateLimitingService;

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<QuestionResponseDto> createComment(
            @PathVariable Integer answerId, // ✅ answerId is now from the path
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
            @PathVariable Integer answerId, // ✅ Included for context
            @PathVariable Integer commentId,
            @RequestPart("comment") @Valid CommentUpdateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> newFiles,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
            
        commentService.updateComment(commentId, request, newFiles, currentUser);
        
        Integer questionId = questionService.findQuestionIdByCommentId(commentId);
        QuestionResponseDto updatedQuestion = questionService.getQuestionById(questionId, currentUser);
        return ResponseEntity.ok(updatedQuestion);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Integer answerId, // ✅ Included for context
            @PathVariable Integer commentId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
            
        commentService.deleteComment(commentId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{commentId}/vote")
    public ResponseEntity<?> voteOnComment(
            @PathVariable Integer answerId, // ✅ Included for context
            @PathVariable Integer commentId,
            @Valid @RequestBody VoteRequest voteRequest,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        Bucket bucket = rateLimitingService.resolveBucket(currentUser.getId().toString());

            // 2. Then, call tryConsume() on the bucket object itself.
            if (!bucket.tryConsume(1)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "You have exhausted your vote limit. Please try again later."));
            }
        
        voteService.voteOnComment(commentId, currentUser, voteRequest.value());
        
        Integer questionId = questionService.findQuestionIdByCommentId(commentId);
        QuestionResponseDto updatedQuestion = questionService.getQuestionById(questionId, currentUser);
        return ResponseEntity.ok(updatedQuestion);
    }
}