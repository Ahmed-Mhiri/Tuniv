package com.tuniv.backend.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommunityCreateRequest(
    @NotBlank @Size(min = 3, max = 100)
    String name,

    @Size(max = 2000)
    String description,

    Integer universityId, // Optional: for university-specific communities

    @NotNull
    CommunitySettingsDto settings
) {}
