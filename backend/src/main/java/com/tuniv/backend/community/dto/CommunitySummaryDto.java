package com.tuniv.backend.community.dto;

public record CommunitySummaryDto(
    Integer communityId,
    String name,
    String iconUrl, // ADDED
    Integer memberCount,
    Integer topicCount,
    String universityName
) {}