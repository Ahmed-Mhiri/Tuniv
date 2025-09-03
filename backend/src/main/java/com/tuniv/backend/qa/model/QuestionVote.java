package com.tuniv.backend.qa.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "question_votes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionVote implements Vote {

    @EmbeddedId
    private QuestionVoteId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("questionId")
    @JoinColumn(name = "question_id")
    @JsonBackReference("question-votes")
    private Question question;

    @Column(nullable = false)
    private short value;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // --- ADD THESE METHODS MANUALLY ---
    @Override
    public User getUser() {
        return this.user;
    }

    @Override
    public short getValue() {
        return this.value;
    }

    public Question getQuestion() {
        return this.question;
    }

    @Override
@Transient
public Integer getPostId() {
    // ✅ CHANGE: Use getId() from the Post superclass
    return this.question != null ? this.question.getId() : null;
}
    // --- END OF ADDED METHODS ---

    @Embeddable
    @Getter
    @Setter
    public static class QuestionVoteId implements Serializable {
        @Column(name = "user_id")
        private Integer userId;

        @Column(name = "question_id")
        private Integer questionId;
        
        public QuestionVoteId() {}
        public QuestionVoteId(Integer userId, Integer questionId) {
            this.userId = userId;
            this.questionId = questionId;
        }

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