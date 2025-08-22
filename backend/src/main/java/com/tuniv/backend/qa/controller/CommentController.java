package com.tuniv.backend.qa.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping; // <-- IMPORT ADDED
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile; // <-- IMPORT ADDED

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.CommentCreateRequest;
import com.tuniv.backend.qa.dto.CommentResponseDto;
import com.tuniv.backend.qa.service.CommentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/answers/{answerId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // --- ENDPOINT UPDATED FOR FILE UPLOADS ---
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<CommentResponseDto> createComment(
            @PathVariable Integer answerId,
            @RequestPart("comment") @Valid CommentCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        
        CommentResponseDto newComment = commentService.createComment(answerId, request, currentUser, files);
        return new ResponseEntity<>(newComment, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CommentResponseDto>> getCommentsByAnswer(@PathVariable Integer answerId) {
        return ResponseEntity.ok(commentService.getCommentsByAnswer(answerId));
    }
}