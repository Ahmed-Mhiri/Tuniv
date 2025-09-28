package com.tuniv.backend.follow.dto;

import com.tuniv.backend.user.dto.UserSummaryDto;
import java.time.Instant;

public record FollowerInfoDto(
    UserSummaryDto user,
    Instant followedAt
) {}
