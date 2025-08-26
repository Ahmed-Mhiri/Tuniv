package com.tuniv.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
public record LoginRequest(
    @NotBlank(message = "Username is required")
    String username,

    @NotBlank(message = "Password is required")
    String password,

    String code // Optional: For 2FA verification
) {}