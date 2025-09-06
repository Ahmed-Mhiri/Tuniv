package com.tuniv.backend.qa.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.AnswerResponseDto;
import com.tuniv.backend.qa.dto.AnswerUpdateRequest;
import com.tuniv.backend.qa.model.Answer;
import com.tuniv.backend.qa.service.AnswerService;

import jakarta.validation.Valid;
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
    // ✨ --- NEW: UPDATE AN ANSWER --- ✨
    @PutMapping(value = "/{answerId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<AnswerResponseDto> updateAnswer(
            @PathVariable Integer answerId,
            @RequestPart("answer") @Valid AnswerUpdateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> newFiles,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
                
        AnswerResponseDto updatedAnswer = answerService.updateAnswer(answerId, request, newFiles, currentUser);
        return ResponseEntity.ok(updatedAnswer);
    }

    // ✨ --- NEW: DELETE AN ANSWER --- ✨
    @DeleteMapping("/{answerId}")
    public ResponseEntity<Void> deleteAnswer(
            @PathVariable Integer answerId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
                
        answerService.deleteAnswer(answerId, currentUser);
        return ResponseEntity.noContent().build();
    }
}