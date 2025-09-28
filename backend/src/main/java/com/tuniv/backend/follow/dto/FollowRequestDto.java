package com.tuniv.backend.follow.dto;

import com.tuniv.backend.follow.model.FollowableType;
import jakarta.validation.constraints.NotNull;

public record FollowRequestDto(
    @NotNull
    FollowableType targetType,

    @NotNull
    Integer targetId
) {}
