package com.tuniv.backend.qa.model;

import com.tuniv.backend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "answer_votes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerVote {

    @EmbeddedId
    private AnswerVoteId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("answerId")
    private Answer answer;

    @Column(nullable = false)
    private int value; // +1 for upvote, -1 for downvote

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerVoteId implements Serializable {
        private Integer userId;
        private Integer answerId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AnswerVoteId that = (AnswerVoteId) o;
            return Objects.equals(userId, that.userId) && Objects.equals(answerId, that.answerId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, answerId);
        }
    }
}