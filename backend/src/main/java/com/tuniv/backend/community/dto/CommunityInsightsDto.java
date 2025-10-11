package com.tuniv.backend.community.dto;

import java.util.List;
import java.util.Map;

public record CommunityInsightsDto(
    // Growth Metrics
    int totalMembers,
    double weeklyGrowthRate,
    Map<String, Long> newMembersByDay, // Key: "YYYY-MM-DD", Value: count

    // Activity Metrics
    int dailyActiveMembers,
    int topicsThisWeek,
    int repliesThisWeek,

    // Top Content & Contributors
    List<CommunityMemberDto> topContributors
) {}
