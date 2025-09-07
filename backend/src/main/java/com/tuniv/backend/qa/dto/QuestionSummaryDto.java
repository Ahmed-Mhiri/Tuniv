package com.tuniv.backend.qa.dto;

import java.time.Instant;

/**
 * A lightweight Data Transfer Object representing a question summary for list views.
 * Using a Java Record makes it concise and immutable.
 */
public record QuestionSummaryDto(
    Integer id,
    String title,
    Integer authorId,
    String authorUsername,
    Instant createdAt,
    int score,
    long answerCount,   // We get the COUNT of answers, not the full answer objects
    int currentUserVote // This will be populated in a second step
) {
    /**
     * A helper method to create a new DTO with the user's vote populated.
     * Since records are immutable, this creates a copy with the updated value.
     */
    public QuestionSummaryDto withCurrentUserVote(int vote) {
        return new QuestionSummaryDto(
            this.id, this.title, this.authorId, this.authorUsername,
            this.createdAt, this.score, this.answerCount, vote
        );
    }
}
