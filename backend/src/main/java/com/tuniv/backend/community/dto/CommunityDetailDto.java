package com.tuniv.backend.community.dto;

import java.time.Instant;

import com.tuniv.backend.user.dto.UserSummaryDto;

public record CommunityDetailDto(
    Integer communityId,
    String name,
    String description,
    Integer memberCount,
    Integer questionCount, // ✅ Added
    Instant createdAt,
    UserSummaryDto creator,
    String universityName,
    boolean isCurrentUserMember,
    boolean isCurrentUserModerator
) {}