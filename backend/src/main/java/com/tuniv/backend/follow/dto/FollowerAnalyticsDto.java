package com.tuniv.backend.follow.dto;

import java.util.Map;

public record FollowerAnalyticsDto(
    int totalFollowers,
    int newFollowersThisWeek,
    int mutualFollowersCount,

    // Provides a breakdown of followers by a certain category, e.g., by university.
    // Key: "University of Bochum", Value: 150
    Map<String, Long> followerBreakdown
) {}