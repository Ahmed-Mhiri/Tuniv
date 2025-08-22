package com.tuniv.backend.qa.model;

import com.tuniv.backend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "question_votes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionVote {

    @EmbeddedId
    private QuestionVoteId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("questionId")
    private Question question;

    @Column(nullable = false)
    private int value; // +1 for upvote, -1 for downvote

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionVoteId implements Serializable {
        private Integer userId;
        private Integer questionId;

        // --- THIS IS THE FIX ---
        // Manually adding the constructor solves the first error.

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QuestionVoteId that = (QuestionVoteId) o;
            return Objects.equals(userId, that.userId) && Objects.equals(questionId, that.questionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, questionId);
        }
    }
}