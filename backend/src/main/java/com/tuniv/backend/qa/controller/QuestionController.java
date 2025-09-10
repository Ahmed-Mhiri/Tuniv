package com.tuniv.backend.qa.controller;

import java.util.List;
import java.util.Map;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType; // <-- IMPORT ADDED
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // <-- IMPORT ADDED
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.AnswerCreateRequest;
import com.tuniv.backend.qa.dto.AnswerUpdateRequest;
import com.tuniv.backend.qa.dto.QuestionCreateRequest;
import com.tuniv.backend.qa.dto.QuestionResponseDto;
import com.tuniv.backend.qa.dto.QuestionSummaryDto;
import com.tuniv.backend.qa.dto.QuestionUpdateRequest;
import com.tuniv.backend.qa.dto.VoteRequest;
import com.tuniv.backend.qa.service.AnswerService;
import com.tuniv.backend.qa.service.QuestionService;
import com.tuniv.backend.qa.service.VoteService;
import com.tuniv.backend.shared.service.RateLimitingService;

import io.github.bucket4j.Bucket;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final AnswerService answerService;
    private final VoteService voteService;
    private final RateLimitingService rateLimitingService;
    private final CacheManager cacheManager; // ✅ Injected for manual cache eviction

    // =================================================================
    // == Question Endpoints
    // =================================================================

    @GetMapping
    public ResponseEntity<Page<QuestionSummaryDto>> getQuestions(
            @RequestParam Integer moduleId,
            @PageableDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        Page<QuestionSummaryDto> questionPage = questionService.getQuestionsByModule(moduleId, pageable, currentUser);
        return ResponseEntity.ok(questionPage);
    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<QuestionResponseDto> createQuestion(
            @RequestPart("question") @Valid QuestionCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        QuestionResponseDto newQuestionDto = questionService.createQuestion(request, currentUser, files);
        return new ResponseEntity<>(newQuestionDto, HttpStatus.CREATED);
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<QuestionResponseDto> getQuestionById(
            @PathVariable Integer questionId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        QuestionResponseDto question = questionService.getQuestionById(questionId, currentUser);
        return ResponseEntity.ok(question);
    }

    @PutMapping(value = "/{questionId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<QuestionResponseDto> updateQuestion(
            @PathVariable Integer questionId,
            @RequestPart("question") @Valid QuestionUpdateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> newFiles,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        QuestionResponseDto updatedQuestion = questionService.updateQuestion(questionId, request, newFiles, currentUser);
        return ResponseEntity.ok(updatedQuestion);
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable Integer questionId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        questionService.deleteQuestion(questionId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{questionId}/vote")
    public ResponseEntity<?> voteOnQuestion(
            @PathVariable Integer questionId,
            @Valid @RequestBody VoteRequest voteRequest,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        Bucket bucket = rateLimitingService.resolveBucket(currentUser.getId().toString());

        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "You have exhausted your vote limit. Please try again later."));
        }

        voteService.voteOnQuestion(questionId, currentUser, (short) voteRequest.value());

        // ✅ THE FIX: Evict the stale question from the cache before fetching.
        Cache questionsCache = cacheManager.getCache("questions");
        if (questionsCache != null) {
            questionsCache.evict(questionId);
        }

        QuestionResponseDto updatedQuestion = questionService.getQuestionById(questionId, currentUser);
        return ResponseEntity.ok(updatedQuestion);
    }

    // =================================================================
    // == Answer Sub-Resource Endpoints (/questions/{qId}/answers/...)
    // =================================================================

    @PostMapping(value = "/{questionId}/answers", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<QuestionResponseDto> addAnswer(
            @PathVariable Integer questionId,
            @RequestPart("answer") @Valid AnswerCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        QuestionResponseDto updatedQuestion = questionService.addAnswer(questionId, request, currentUser, files);
        return new ResponseEntity<>(updatedQuestion, HttpStatus.CREATED);
    }

    @PutMapping(value = "/{questionId}/answers/{answerId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<QuestionResponseDto> updateAnswer(
            @PathVariable Integer questionId,
            @PathVariable Integer answerId,
            @RequestPart("answer") @Valid AnswerUpdateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> newFiles,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        answerService.updateAnswer(answerId, request, newFiles, currentUser);
        QuestionResponseDto updatedQuestion = questionService.getQuestionById(questionId, currentUser);
        return ResponseEntity.ok(updatedQuestion);
    }

    @DeleteMapping("/{questionId}/answers/{answerId}")
    public ResponseEntity<Void> deleteAnswer(
            @PathVariable Integer questionId,
            @PathVariable Integer answerId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        answerService.deleteAnswer(answerId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{questionId}/answers/{answerId}/solution")
    public ResponseEntity<QuestionResponseDto> markAsSolution(
            @PathVariable Integer questionId,
            @PathVariable Integer answerId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        answerService.markAsSolution(answerId, currentUser);
        QuestionResponseDto updatedQuestion = questionService.getQuestionById(questionId, currentUser);
        return ResponseEntity.ok(updatedQuestion);
    }

    @PostMapping("/{questionId}/answers/{answerId}/vote")
    public ResponseEntity<?> voteOnAnswer(
            @PathVariable Integer questionId,
            @PathVariable Integer answerId,
            @Valid @RequestBody VoteRequest voteRequest,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        Bucket bucket = rateLimitingService.resolveBucket(currentUser.getId().toString());

        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "You have exhausted your vote limit. Please try again later."));
        }

        voteService.voteOnAnswer(answerId, currentUser, (short) voteRequest.value());

        // ✅ THE FIX: Evict the stale question from the cache before fetching.
        Cache questionsCache = cacheManager.getCache("questions");
        if (questionsCache != null) {
            questionsCache.evict(questionId);
        }

        QuestionResponseDto updatedQuestion = questionService.getQuestionById(questionId, currentUser);
        return ResponseEntity.ok(updatedQuestion);
    }
}