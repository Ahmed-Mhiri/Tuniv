package com.tuniv.backend.auth.dto;

public record JwtResponse(
    String token,
    Integer userId,
    String username,
    String email,
    String profilePhotoUrl,
    String bio,
    String major,
    Integer reputationScore,
    // --- NEW FIELDS ---
    long questionsCount,
    long answersCount,
    long followersCount,
    // --- EXISTING 2FA FIELDS ---
    boolean is2faRequired,
    boolean is2faEnabled
) {}