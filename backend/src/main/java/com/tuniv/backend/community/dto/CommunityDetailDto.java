package com.tuniv.backend.community.dto;

import java.time.Instant;

import com.tuniv.backend.user.dto.UserSummaryDto;

public record CommunityDetailDto(
    Integer communityId,
    String name,
    String description,
    Integer memberCount,
    Integer topicCount, // ✅ UPDATED: questionCount → topicCount
    Instant createdAt,
    UserSummaryDto creator,
    String universityName,
    boolean isMember,
    boolean isModerator
) {}