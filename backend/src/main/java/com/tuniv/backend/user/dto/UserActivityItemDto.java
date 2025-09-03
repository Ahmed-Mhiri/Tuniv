package com.tuniv.backend.user.dto;

import java.time.Instant; // This is correct

public record UserActivityItemDto(
    ActivityType type,
    
    // THE FIX IS ON THIS LINE: Changed from Instant back to LocalDateTime
    Instant createdAt, // This is correct
    
    Integer postScore,
    Integer voteValue,
    Integer questionId,
    String questionTitle,
    Integer answerId,
    boolean isSolution,
    Integer commentId
) {
    public enum ActivityType {
        QUESTION_ASKED,
        ANSWER_POSTED,
        COMMENT_POSTED,
        ACCEPTED_AN_ANSWER,
        VOTE_CAST
    }
}