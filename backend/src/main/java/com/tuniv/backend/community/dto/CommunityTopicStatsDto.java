package com.tuniv.backend.community.dto;

public record CommunityTopicStatsDto(
    Integer communityId,
    String communityName,
    Long totalTopics,
    Long solvedTopics,
    Long questionTopics,
    Long postTopics
) {
    public Double getSolutionRate() {
        if (totalTopics == 0) return 0.0;
        return (solvedTopics.doubleValue() / totalTopics.doubleValue()) * 100;
    }

    public Double getQuestionPercentage() {
        if (totalTopics == 0) return 0.0;
        return (questionTopics.doubleValue() / totalTopics.doubleValue()) * 100;
    }

    public Double getPostPercentage() {
        if (totalTopics == 0) return 0.0;
        return (postTopics.doubleValue() / totalTopics.doubleValue()) * 100;
    }
}
