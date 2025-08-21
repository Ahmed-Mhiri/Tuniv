package com.tuniv.backend.user.dto;

public record UserProfileDto(
    Integer userId,
    String username,
    String profilePhotoUrl,
    String bio,
    String major,
    Integer reputationScore
    // We could add a list of university memberships here in the future
) {}