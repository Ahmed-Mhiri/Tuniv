package com.tuniv.backend.community.dto;

public record TrendingCommunityDto(
    Integer communityId,
    String name,
    Integer memberCount,
    Integer topicCount,
    String universityName,
    Long recentActivityCount,
    Double activityScore
) {}
