package com.tuniv.backend.community.dto;

public record CommunityCreateRequest(
    String name,
    String description,
    Integer universityId // Nullable. If provided, creates a University-Specific community.
) {}
