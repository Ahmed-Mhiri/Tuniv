
package com.tuniv.backend.community.dto;

public record CommunityRuleDto(
    Integer ruleId,
    int displayOrder,
    String title,
    String description
) {}