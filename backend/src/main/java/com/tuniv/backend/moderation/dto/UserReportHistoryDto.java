package com.tuniv.backend.moderation.dto;

import com.tuniv.backend.moderation.model.ReportStatus;
import java.time.Instant;

public record UserReportHistoryDto(
    Integer reportId,
    String reportedContentSummary, // e.g., "Your post in 'General Discussion'"
    String reason,
    Instant reportDate,
    ReportStatus status
) {}