package com.tuniv.backend.user.dto;

public record UserSummaryDto(
    Integer id,
    String username,
    String profilePhotoUrl,
    Integer reputationScore
) {}
