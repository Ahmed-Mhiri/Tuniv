package com.tuniv.backend.community.dto;

public record CommunityUpdateRequest(
    String name,
    String description
) {}