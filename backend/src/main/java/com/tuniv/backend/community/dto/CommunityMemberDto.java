package com.tuniv.backend.community.dto;

import com.tuniv.backend.community.model.CommunityRole;
import com.tuniv.backend.user.dto.UserSummaryDto;
import java.time.Instant;

public record CommunityMemberDto(
    UserSummaryDto user,
    CommunityRole role,
    Instant joinedAt,
    Integer contributionScore
) {}