package com.tuniv.backend.community.dto;

import java.util.Map;

import com.tuniv.backend.qa.model.TopicType;

public record CommunityStatsDto(
    Integer communityId,
    String communityName,
    Integer totalTopics,
    Integer totalMembers,
    Integer solvedTopics,
    Map<TopicType, Long> topicTypeDistribution,
    Double solutionRate,
    Double questionPercentage,
    Double postPercentage
) {}
