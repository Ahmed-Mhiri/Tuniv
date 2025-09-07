package com.tuniv.backend.qa.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.QuestionResponseDto;
import com.tuniv.backend.qa.dto.QuestionSummaryDto;
import com.tuniv.backend.qa.service.FeedService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    // ✅ UPDATED return type
    public Page<QuestionSummaryDto> getMyFeed(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            Pageable pageable) {
        return feedService.getPersonalizedFeed(currentUser, pageable);
    }

    @GetMapping("/popular")
    // ✅ UPDATED return type
    public Page<QuestionSummaryDto> getPopularFeed(
            Pageable pageable,
            // Pass the current user (can be null for guests) to show their vote status
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return feedService.getPopularFeed(pageable, currentUser);
    }
}
