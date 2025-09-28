package com.tuniv.backend.community.dto;

public record CommunitySummaryDto(
    Integer communityId,
    String name,
    Integer memberCount,
    Integer questionCount, // ✅ Added
    String universityName
) {}
