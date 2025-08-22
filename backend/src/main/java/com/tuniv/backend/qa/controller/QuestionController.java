package com.tuniv.backend.qa.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.AnswerCreateRequest;
import com.tuniv.backend.qa.dto.QuestionCreateRequest;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.model.Question;
import com.tuniv.backend.qa.service.QuestionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping(value = "/modules/{moduleId}/questions", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Question> createQuestion(
            @PathVariable Integer moduleId,
            @RequestPart("question") @Valid QuestionCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        
        Question newQuestion = questionService.createQuestion(request, moduleId, currentUser, files);
        return new ResponseEntity<>(newQuestion, HttpStatus.CREATED);
    }

    @GetMapping("/modules/{moduleId}/questions")
    public ResponseEntity<List<Question>> getQuestionsByModule(@PathVariable Integer moduleId) {
        List<Question> questions = questionService.getQuestionsByModule(moduleId);
        return ResponseEntity.ok(questions);
    }

    @PostMapping(value = "/questions/{questionId}/answers", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Answer> addAnswer(
            @PathVariable Integer questionId,
            @RequestPart("answer") @Valid AnswerCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
                
        Answer newAnswer = questionService.addAnswer(request, questionId, currentUser, files);
        return new ResponseEntity<>(newAnswer, HttpStatus.CREATED);
    }
}