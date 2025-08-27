package com.tuniv.backend.qa.controller; // Or your correct package

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.VoteRequest;
import com.tuniv.backend.qa.service.VoteService;
import com.tuniv.backend.shared.service.RateLimitingService; // <-- Import Map

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;
    private final RateLimitingService rateLimitingService;

    @PostMapping("/questions/{questionId}/vote")
    public ResponseEntity<?> voteOnQuestion(@PathVariable Integer questionId,
                                            @Valid @RequestBody VoteRequest voteRequest,
                                            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        // ... rate limiting logic ...

        short value = (short) voteRequest.value(); // Cast to short
        voteService.voteOnQuestion(questionId, currentUser, value);
        // --- FIX: Return a JSON object ---
        return ResponseEntity.ok(Map.of("message", "Vote registered successfully."));
    }

    @PostMapping("/answers/{answerId}/vote")
    public ResponseEntity<?> voteOnAnswer(@PathVariable Integer answerId,
                                          @Valid @RequestBody VoteRequest voteRequest,
                                          @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        // ... rate limiting logic ...
                                          
        short value = (short) voteRequest.value(); // Cast to short
        voteService.voteOnAnswer(answerId, currentUser, value);
        // --- FIX: Return a JSON object ---
        return ResponseEntity.ok(Map.of("message", "Vote registered successfully."));
    }

    @PostMapping("/comments/{commentId}/vote")
    public ResponseEntity<?> voteOnComment(@PathVariable Integer commentId,
                                           @Valid @RequestBody VoteRequest voteRequest,
                                           @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        // ... rate limiting logic ...

        short value = (short) voteRequest.value(); // Cast to short
        voteService.voteOnComment(commentId, currentUser, value);
        // --- FIX: Return a JSON object ---
        return ResponseEntity.ok(Map.of("message", "Vote on comment registered successfully."));
    }
}