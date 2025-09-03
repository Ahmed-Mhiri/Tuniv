package com.tuniv.backend.qa.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "comment_votes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentVote implements Vote {

    @EmbeddedId
    private CommentVoteId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("commentId")
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @Column(nullable = false)
    private short value;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // --- ADD THESE METHODS MANUALLY TO FIX THE ERROR ---
    @Override
    public User getUser() {
        return this.user;
    }
    
    @Override
    public short getValue() {
        return this.value;
    }

    public Comment getComment() {
        return this.comment;
    }
    
    @Override
    @Transient // This tells JPA to ignore this method for database mapping
    public Integer getPostId() {
        // Return the ID of the comment this vote belongs to
        return this.comment != null ? this.comment.getCommentId() : null;
    }
    // --- END OF ADDED METHODS ---


    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentVoteId implements Serializable {
        @Column(name = "user_id")
        private Integer userId;
        @Column(name = "comment_id")
        private Integer commentId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CommentVoteId that = (CommentVoteId) o;
            return Objects.equals(userId, that.userId) && Objects.equals(commentId, that.commentId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, commentId);
        }
    }
}