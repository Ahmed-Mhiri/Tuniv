package com.tuniv.backend.community.dto;

import com.tuniv.backend.user.dto.UserSummaryDto;
import java.time.Instant;

public record CommunityJoinRequestDto(
    Integer requestId,
    UserSummaryDto user,
    Instant requestedAt,
    // Optional: A message from the user explaining why they want to join
    String message
) {}