package com.tuniv.backend.qa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.service.AnswerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @PostMapping("/{answerId}/solution")
    public ResponseEntity<?> markAsSolution(@PathVariable Integer answerId,
                                            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Answer updatedAnswer = answerService.markAsSolution(answerId, currentUser);
        return ResponseEntity.ok(updatedAnswer);
    }
}