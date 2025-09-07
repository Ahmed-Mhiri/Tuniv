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
@Table(name = "answer_votes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerVote implements Vote {

    @EmbeddedId
    private AnswerVoteId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("answerId")
    @JoinColumn(name = "answer_id")
    @JsonBackReference("answer-votes")
    private Answer answer;

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

    public Answer getAnswer() {
        return this.answer;
    }

    @Override
@Transient
public Integer getPostId() {
    // ✅ CHANGE: Use getId() from the Post superclass
    return this.answer != null ? this.answer.getId() : null;
}
    // --- END OF ADDED METHODS ---

    @Embeddable
    @Getter
    @Setter
    public static class AnswerVoteId implements Serializable {
        @Column(name = "user_id")
        private Integer userId;
        
        @Column(name = "answer_id")
        private Integer answerId;

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
    
    @Override
public Post getPost() { // ✅ ADD THIS METHOD
    return this.answer;
}
}