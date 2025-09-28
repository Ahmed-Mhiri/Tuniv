package com.tuniv.backend.qa.dto;

import java.time.Instant;
import java.util.List;

import com.tuniv.backend.qa.model.VoteStatus;


public record QuestionSummaryDto(
    Integer id,
    String title,
    Integer authorId,
    String authorUsername,
    Instant createdAt,
    int score,
    int answerCount,
    VoteStatus userVoteStatus,
    boolean hasAcceptedAnswer,
    List<String> tags,
    Integer universityId,
    String universityName
) {
    /**
     * ✅ ADD THIS CONSTRUCTOR
     * This constructor will be called by the JPQL query.
     * It accepts the raw String from the CASE statement and converts it to the VoteStatus enum.
     */
    public QuestionSummaryDto(
            Integer id, String title, Integer authorId, String authorUsername, Instant createdAt,
            int score, int answerCount, String userVoteStatusString, boolean hasAcceptedAnswer,
            Integer universityId, String universityName
    ) {
        this(id, title, authorId, authorUsername, createdAt, score, answerCount,
             VoteStatus.fromString(userVoteStatusString), // Convert String to Enum
             hasAcceptedAnswer, List.of(), universityId, universityName);
    }

    public QuestionSummaryDto withTags(List<String> newTags) {
        return new QuestionSummaryDto(
            this.id, this.title, this.authorId, this.authorUsername, 
            this.createdAt, this.score, this.answerCount, this.userVoteStatus,
            this.hasAcceptedAnswer, newTags, this.universityId, this.universityName
        );
    }
}
