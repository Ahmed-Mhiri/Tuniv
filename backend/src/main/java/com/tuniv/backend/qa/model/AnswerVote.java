package com.tuniv.backend.qa.model;

import java.io.Serializable;
import java.util.Objects;

import com.tuniv.backend.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @JoinColumn(name = "user_id") // <-- ADDED THIS
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("answerId")
    @JoinColumn(name = "answer_id") // <-- ADDED THIS
    private Answer answer;

    @Column(nullable = false)
    private int value;

    @Embeddable
    @Getter
    @Setter
    public static class AnswerVoteId implements Serializable {
        
        @Column(name = "user_id") // <-- ADDED THIS
        private Integer userId;
        
        @Column(name = "answer_id") // <-- ADDED THIS
        private Integer answerId;

        // Constructors, equals, and hashCode...
        public AnswerVoteId() {}
        public AnswerVoteId(Integer userId, Integer answerId) {
            this.userId = userId;
            this.answerId = answerId;
        }

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