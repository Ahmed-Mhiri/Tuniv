package com.tuniv.backend.community.dto;

import java.time.Instant;

import com.tuniv.backend.user.dto.UserSummaryDto;

public record CommunityDetailDto(
    Integer communityId,
    String name,
    String description,
    String bannerUrl, // ADDED
    String iconUrl,   // ADDED
    Integer memberCount,
    Integer followerCount, // ADDED
    Integer topicCount,
    Instant createdAt,
    UserSummaryDto creator,
    String universityName,
    boolean isVerified, // ADDED
    CommunitySettingsDto settings, // ADDED: All policies in one object
    CommunityMembershipStatusDto membership // ADDED: Detailed status for the current user
) {}