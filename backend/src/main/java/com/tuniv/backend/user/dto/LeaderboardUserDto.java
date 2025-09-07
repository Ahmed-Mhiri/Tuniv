package com.tuniv.backend.user.dto;

public record LeaderboardUserDto(
    Integer userId,
    String username,
    Integer reputationScore,
    String profilePhotoUrl
) {}
