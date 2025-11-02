package com.tuniv.backend.qa.dto;

import java.time.Instant;
import java.util.List;

import com.tuniv.backend.qa.model.TopicType;

public record TopicSummaryDto(
    Integer id,
    String title,
    TopicType topicType,
    Integer authorId,
    String authorUsername,
    Instant createdAt,
    int score,
    int replyCount,
    String currentUserVote, // "UPVOTE", "DOWNVOTE", or null
    boolean isSolved,
    Integer containerId,
    String containerName,
    List<String> tags
) {
    public TopicSummaryDto withTags(List<String> newTags) {
        return new TopicSummaryDto(
            id, title, topicType, authorId, authorUsername, createdAt, score, 
            replyCount, currentUserVote, isSolved, containerId, containerName, newTags
        );
    }
}