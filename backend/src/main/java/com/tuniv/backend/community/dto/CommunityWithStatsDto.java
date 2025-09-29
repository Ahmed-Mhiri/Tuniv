package com.tuniv.backend.community.dto;

public record CommunityWithStatsDto(
    CommunityDetailDto community,
    CommunityStatsDto stats
) {}
