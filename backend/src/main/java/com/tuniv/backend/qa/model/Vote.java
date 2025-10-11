package com.tuniv.backend.qa.model;


import java.time.Instant;

import org.hibernate.annotations.Where;

import com.tuniv.backend.shared.model.Auditable;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "votes", indexes = {
    @Index(name = "idx_vote_user_post", columnList = "user_id, post_id", unique = true),
    @Index(name = "idx_vote_post", columnList = "post_id, value"),
    @Index(name = "idx_vote_created", columnList = "created_at DESC"),
    @Index(name = "idx_vote_user_created", columnList = "user_id, created_at DESC"),
    @Index(name = "idx_vote_university", columnList = "university_context_id"),
    @Index(name = "idx_vote_post_user_value", columnList = "post_id, user_id, value") // ✅ ADDED
})
@Getter
@Setter
@NoArgsConstructor
@Where(clause = "is_retracted = false") // ✅ ADD: Global filter for non-retracted votes

public class Vote extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ========== OPTIMISTIC LOCKING ==========
    @Version
    private Long version;

    // ========== CORE RELATIONSHIPS ==========
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private VotablePost post;

    // ========== VOTE DATA ==========
    @Column(name = "value", nullable = false)
    private short value; // +1 for upvote, -1 for downvote

    @Column(name = "weight", nullable = false)
    private Integer weight = 1;

    @Column(name = "is_retracted", nullable = false)
    private boolean isRetracted = false;

    @Column(name = "retracted_at")
    private Instant retractedAt;

    @Column(name = "retraction_reason")
    private String retractionReason;

    // ========== CONTEXT & METADATA ==========
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_context_id")
    private University universityContext;

    @Column(name = "vote_strength")
    private Double voteStrength = 1.0; // For future algorithmic weighting

}



