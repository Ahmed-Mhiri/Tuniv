package com.tuniv.backend.user.dto;

public record UserProfileDto(
    Integer userId,
    String username,
    String profilePhotoUrl,
    String bio,
    String major,
    Integer reputationScore,
    long questionsCount,
    long answersCount,
    long followersCount,
    VerificationInfo verification // ✅ ADD THIS FIELD

) {}