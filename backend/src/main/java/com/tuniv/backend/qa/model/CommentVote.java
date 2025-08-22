package com.tuniv.backend.qa.model;

import java.io.Serializable;
import java.util.Objects;

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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "comment_votes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentVote {

    @EmbeddedId
    private CommentVoteId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    @JsonBackReference("user-comment-votes")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("commentId")
    @JoinColumn(name = "comment_id")
    @JsonBackReference("comment-votes")
    private Comment comment;

    @Column(nullable = false)
    private int value;

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

        // --- FULL IMPLEMENTATION OF equals and hashCode ---
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