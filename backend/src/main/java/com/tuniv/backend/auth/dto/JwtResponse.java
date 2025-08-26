package com.tuniv.backend.auth.dto;

public record JwtResponse(
    String token,
    Integer userId, // <-- Renamed from 'id'
    String username,
    String email,
    String profilePhotoUrl,
    String bio,             // <-- ADD
    String major,           // <-- ADD
    Integer reputationScore, // <-- ADD
    boolean is2faRequired,
    boolean is2faEnabled
) {}