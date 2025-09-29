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
        TOPIC_CREATED,      // User created a topic
    ANSWER_POSTED,      // User posted an answer (reply on QUESTION topic)
    COMMENT_POSTED,     // User posted a comment (reply on POST topic or nested reply)
    SOLUTION_ACCEPTED,  // User's answer was accepted as solution
    VOTE_CAST          // User voted on something
    }
}