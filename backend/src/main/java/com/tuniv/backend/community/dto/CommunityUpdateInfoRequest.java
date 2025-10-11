package com.tuniv.backend.community.dto;

import jakarta.validation.constraints.Size;

public record CommunityUpdateInfoRequest(
    @Size(min = 3, max = 100) String name,
    @Size(max = 2000) String description,
    String bannerUrl,
    String iconUrl
) {}