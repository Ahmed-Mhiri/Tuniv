package com.tuniv.backend.community.dto;

import com.tuniv.backend.user.dto.UserSummaryDto;
import java.time.Instant;

public record BannedMemberDto(
    UserSummaryDto user,
    String banReason,
    Instant bannedAt,
    UserSummaryDto bannedBy
) {}