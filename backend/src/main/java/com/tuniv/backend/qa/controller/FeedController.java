package com.tuniv.backend.qa.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.QuestionResponseDto;
import com.tuniv.backend.qa.service.FeedService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping
    public ResponseEntity<Page<QuestionResponseDto>> getFeed(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        
        Page<QuestionResponseDto> feedPage = feedService.getPersonalizedFeed(currentUser, pageable);
        return ResponseEntity.ok(feedPage);
    }
}
