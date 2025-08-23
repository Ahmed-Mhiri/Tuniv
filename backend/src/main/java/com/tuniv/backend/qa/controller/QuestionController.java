package com.tuniv.backend.qa.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // <-- IMPORT ADDED
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType; // <-- IMPORT ADDED
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
import com.tuniv.backend.qa.dto.AnswerResponseDto;
import com.tuniv.backend.qa.dto.QuestionCreateRequest;
import com.tuniv.backend.qa.dto.QuestionResponseDto;
import com.tuniv.backend.qa.service.QuestionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping(value = "/modules/{moduleId}/questions", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<QuestionResponseDto> createQuestion( // <-- RETURN TYPE CHANGED
            @PathVariable Integer moduleId,
            @RequestPart("question") @Valid QuestionCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        
        QuestionResponseDto newQuestionDto = questionService.createQuestion(request, moduleId, currentUser, files);
        return new ResponseEntity<>(newQuestionDto, HttpStatus.CREATED);
    }

    @GetMapping("/modules/{moduleId}/questions")
    public ResponseEntity<Page<QuestionResponseDto>> getQuestionsByModule(
            @PathVariable Integer moduleId, Pageable pageable) {
        
        return ResponseEntity.ok(questionService.getQuestionsByModule(moduleId, pageable));
    }


    @GetMapping("/questions/{questionId}")
    public ResponseEntity<QuestionResponseDto> getQuestionById(@PathVariable Integer questionId) {
        return ResponseEntity.ok(questionService.getQuestionDtoById(questionId));
    }

    @PostMapping(value = "/questions/{questionId}/answers", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<AnswerResponseDto> addAnswer( // <-- RETURN TYPE CHANGED
            @PathVariable Integer questionId,
            @RequestPart("answer") @Valid AnswerCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
                
        AnswerResponseDto newAnswerDto = questionService.addAnswer(request, questionId, currentUser, files);
        return new ResponseEntity<>(newAnswerDto, HttpStatus.CREATED);
    }
}