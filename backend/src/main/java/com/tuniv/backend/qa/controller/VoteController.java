package com.tuniv.backend.qa.controller;

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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PostMapping("/questions/{questionId}/vote")
    public ResponseEntity<?> voteOnQuestion(@PathVariable Integer questionId,
                                            @Valid @RequestBody VoteRequest voteRequest,
                                            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        int value = (voteRequest.value() >= 0) ? 1 : -1;
        voteService.voteOnQuestion(questionId, currentUser, value);
        return ResponseEntity.ok("Vote registered successfully.");
    }

    @PostMapping("/answers/{answerId}/vote")
    public ResponseEntity<?> voteOnAnswer(@PathVariable Integer answerId,
                                          @Valid @RequestBody VoteRequest voteRequest,
                                          @AuthenticationPrincipal UserDetailsImpl currentUser) {

        int value = (voteRequest.value() >= 0) ? 1 : -1;
        voteService.voteOnAnswer(answerId, currentUser, value);
        return ResponseEntity.ok("Vote registered successfully.");
    }

    @PostMapping("/comments/{commentId}/vote")
    public ResponseEntity<?> voteOnComment(@PathVariable Integer commentId,
                                           @Valid @RequestBody VoteRequest voteRequest,
                                           @AuthenticationPrincipal UserDetailsImpl currentUser) {
                                               
        int value = (voteRequest.value() >= 0) ? 1 : -1;
        voteService.voteOnComment(commentId, currentUser, value);
        return ResponseEntity.ok("Vote on comment registered successfully.");
    }
}