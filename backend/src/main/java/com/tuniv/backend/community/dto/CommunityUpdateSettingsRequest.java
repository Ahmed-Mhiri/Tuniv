package com.tuniv.backend.community.dto;

import jakarta.validation.constraints.NotNull;

public record CommunityUpdateSettingsRequest(
    @NotNull
    CommunitySettingsDto settings
) {}