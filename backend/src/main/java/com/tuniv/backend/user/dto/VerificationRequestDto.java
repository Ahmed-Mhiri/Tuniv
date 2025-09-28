package com.tuniv.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerificationRequestDto(
    @NotBlank @Email String universityEmail
) {}
