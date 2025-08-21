package com.tuniv.backend.user.dto;

import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
    @Size(max = 500)
    String bio,

    @Size(max = 100)
    String major,

    @Size(max = 255)
    String profilePhotoUrl
) {}