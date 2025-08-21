package com.tuniv.backend.qa.controller;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.AnswerCreateRequest;
import com.tuniv.backend.qa.dto.QuestionCreateRequest;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.qa.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping("/modules/{moduleId}/questions")
    public ResponseEntity<?> createQuestion(@PathVariable Integer moduleId,
                                            @Valid @RequestBody QuestionCreateRequest request,
                                            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            Question newQuestion = questionService.createQuestion(request, moduleId, currentUser);
            return ResponseEntity.ok(newQuestion); // In a real app, you'd return a QuestionResponseDto
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/modules/{moduleId}/questions")
    public ResponseEntity<List<Question>> getQuestionsByModule(@PathVariable Integer moduleId) {
        // Here you would typically use DTOs and Pageable for pagination
        List<Question> questions = questionService.getQuestionsByModule(moduleId);
        return ResponseEntity.ok(questions);
    }

    @PostMapping("/questions/{questionId}/answers")
    public ResponseEntity<?> addAnswer(@PathVariable Integer questionId,
                                       @Valid @RequestBody AnswerCreateRequest request,
                                       @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            Answer newAnswer = questionService.addAnswer(request, questionId, currentUser);
            return ResponseEntity.ok(newAnswer); // Return an AnswerResponseDto
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}