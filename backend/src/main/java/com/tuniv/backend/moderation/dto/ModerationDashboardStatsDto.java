package com.tuniv.backend.moderation.dto;

import java.util.Map;

public record ModerationDashboardStatsDto(
    // Queue Health
    long pendingReports,
    long reportsUnderReview,
    double averageResolutionTimeInHours,

    // Activity Volume
    long reportsFiledToday,
    long reportsResolvedToday,

    // Leaderboards
    Map<String, Long> topReportedUsers, // Key: username, Value: report count
    Map<String, Long> topModerators // Key: username, Value: resolved count
) {}