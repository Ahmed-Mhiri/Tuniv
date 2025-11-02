package com.tuniv.backend.moderation.web;

import com.tuniv.backend.moderation.dto.CreateReportRequest;
import com.tuniv.backend.moderation.dto.HandleReportRequest;
import com.tuniv.backend.moderation.dto.ReportDetailDto;
import com.tuniv.backend.moderation.dto.ReportSummaryDto;
import com.tuniv.backend.moderation.model.ReportScope;
import com.tuniv.backend.moderation.service.ModerationService;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/moderation")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()") // All endpoints require an authenticated user
public class ModerationController {

    private final ModerationService moderationService;
    private final UserRepository userRepository; // To fetch the current user

    /**
     * Creates a new report for a piece of content.
     */
    @PostMapping("/reports")
    public ResponseEntity<ReportSummaryDto> createReport(
            @Valid @RequestBody CreateReportRequest request) {
        
        User reporter = getCurrentUser();
        ReportSummaryDto createdReport = moderationService.createReport(request, reporter);
        return new ResponseEntity<>(createdReport, HttpStatus.CREATED);
    }

    /**
     * Gets the detailed view of a single report.
     * (Accessible by moderators with permission for the report's scope)
     */
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<ReportDetailDto> getReportDetails(
            @PathVariable Integer reportId) {
        
        // Permission check is handled inside the service
        ReportDetailDto reportDetails = moderationService.getReportDetails(reportId);
        return ResponseEntity.ok(reportDetails);
    }

    /**
     * Handles a report (resolves, dismisses, etc.)
     * (Accessible by moderators with permission for the report's scope)
     */
    @PostMapping("/reports/{reportId}/handle")
    public ResponseEntity<ReportDetailDto> handleReport(
            @PathVariable Integer reportId,
            @Valid @RequestBody HandleReportRequest request) {
        
        User moderator = getCurrentUser();
        // Permission check is handled inside the service
        ReportDetailDto updatedReport = moderationService.handleReport(reportId, request, moderator);
        return ResponseEntity.ok(updatedReport);
    }

    /**
     * Gets a paginated list of reports for a specific moderation queue.
     * (Accessible by moderators with permission for that queue)
     */
    @GetMapping("/reports/queue")
    public ResponseEntity<Page<ReportSummaryDto>> getReportsByScope(
            @RequestParam ReportScope scope,
            @RequestParam(required = false) Integer universityId,
            @RequestParam(required = false) Integer communityId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        
        // Permission check for the queue is handled inside the service
        Page<ReportSummaryDto> reportsPage = moderationService.getReportsByScope(
                scope, universityId, communityId, pageable
        );
        return ResponseEntity.ok(reportsPage);
    }

    // --- Helper Method ---

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null) {
            throw new AccessDeniedException("No authenticated user found.");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}