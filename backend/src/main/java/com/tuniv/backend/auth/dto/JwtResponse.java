package com.tuniv.backend.auth.dto;

public record JwtResponse(
    String token,
    Integer id,
    String username,
    String email
) {}