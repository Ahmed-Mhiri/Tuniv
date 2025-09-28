package com.tuniv.backend.follow.dto;
import com.tuniv.backend.follow.model.FollowableType;
import java.time.Instant;
public record FollowSummaryDto(
    Integer followId,
    FollowableType targetType,
    Integer targetId,
    String targetName,
    Instant followedAt
) {}
