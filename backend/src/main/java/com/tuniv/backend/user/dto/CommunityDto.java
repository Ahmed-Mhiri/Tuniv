package com.tuniv.backend.user.dto;

public record CommunityDto(
    Integer id,
    String type, // Will be "UNIVERSITY"
    String name,
    Integer memberCount
) {}
