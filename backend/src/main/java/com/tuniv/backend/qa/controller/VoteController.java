package com.tuniv.backend.qa.controller;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.VoteRequest;
import com.tuniv.backend.qa.service.VoteService;
import com.tuniv.backend.shared.service.RateLimitingService; // <-- IMPORT ADDED
import io.github.bucket4j.Bucket; // <-- IMPORT ADDED
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus; // <-- IMPORT ADDED
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;
    private final RateLimitingService rateLimitingService; // <-- DEPENDENCY ADDED

    @PostMapping("/questions/{questionId}/vote")
    public ResponseEntity<?> voteOnQuestion(@PathVariable Integer questionId,
                                            @Valid @RequestBody VoteRequest voteRequest,
                                            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        Bucket bucket = rateLimitingService.resolveBucket(currentUser.getUsername());
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("You have made too many requests. Please try again in a minute.");
        }

        int value = (voteRequest.value() >= 0) ? 1 : -1;
        voteService.voteOnQuestion(questionId, currentUser, value);
        return ResponseEntity.ok("Vote registered successfully.");
    }

    @PostMapping("/answers/{answerId}/vote")
    public ResponseEntity<?> voteOnAnswer(@PathVariable Integer answerId,
                                          @Valid @RequestBody VoteRequest voteRequest,
                                          @AuthenticationPrincipal UserDetailsImpl currentUser) {
                                              
        Bucket bucket = rateLimitingService.resolveBucket(currentUser.getUsername());
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("You have made too many requests. Please try again in a minute.");
        }
                                              
        int value = (voteRequest.value() >= 0) ? 1 : -1;
        voteService.voteOnAnswer(answerId, currentUser, value);
        return ResponseEntity.ok("Vote registered successfully.");
    }

    @PostMapping("/comments/{commentId}/vote")
    public ResponseEntity<?> voteOnComment(@PathVariable Integer commentId,
                                           @Valid @RequestBody VoteRequest voteRequest,
                                           @AuthenticationPrincipal UserDetailsImpl currentUser) {
                                               
        Bucket bucket = rateLimitingService.resolveBucket(currentUser.getUsername());
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("You have made too many requests. Please try again in a minute.");
        }

        int value = (voteRequest.value() >= 0) ? 1 : -1;
        voteService.voteOnComment(commentId, currentUser, value);
        return ResponseEntity.ok("Vote on comment registered successfully.");
    }
}