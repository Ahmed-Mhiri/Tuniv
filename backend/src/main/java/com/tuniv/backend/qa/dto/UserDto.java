package com.tuniv.backend.qa.dto;

public record UserDto(
    Integer id,
    String username,
    int reputationScore,
    String profilePhotoUrl // ✅ Standardized from avatarUrl
) {}