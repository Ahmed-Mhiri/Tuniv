package com.tuniv.backend.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommunityRuleRequest(
    @NotNull
    int displayOrder,

    @NotBlank @Size(max = 100)
    String title,

    @NotBlank @Size(max = 1000)
    String description
) {}