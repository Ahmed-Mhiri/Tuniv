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
import com.tuniv.backend.qa.service.QuestionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VoteController {

    private final QuestionService questionService;

    @PostMapping("/questions/{questionId}/vote")
    public ResponseEntity<?> voteOnQuestion(@PathVariable Integer questionId,
                                            @Valid @RequestBody VoteRequest voteRequest,
                                            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            // value will be 1 for upvote, -1 for downvote
            int value = (voteRequest.value() > 0) ? 1 : -1;
            questionService.voteOnQuestion(questionId, currentUser, value);
            return ResponseEntity.ok("Vote registered successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/answers/{answerId}/vote")
    public ResponseEntity<?> voteOnAnswer(@PathVariable Integer answerId,
                                          @Valid @RequestBody VoteRequest voteRequest,
                                          @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            int value = (voteRequest.value() > 0) ? 1 : -1;
            questionService.voteOnAnswer(answerId, currentUser, value);
            return ResponseEntity.ok("Vote registered successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}