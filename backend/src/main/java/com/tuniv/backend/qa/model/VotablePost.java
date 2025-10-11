package com.tuniv.backend.qa.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class VotablePost extends Post {

    // ========== VOTING STATISTICS ==========
    @Column(name = "score", nullable = false)
    private Integer score = 0;

    @Column(name = "upvote_count", nullable = false)
    private Integer upvoteCount = 0;

    @Column(name = "downvote_count", nullable = false)
    private Integer downvoteCount = 0;

    @Column(name = "vote_ratio")
    private Double voteRatio = 0.0;

    // ========== ENGAGEMENT METRICS ==========
    @Column(name = "university_engagement_score")
    private Double universityEngagementScore = 0.0;

    @Column(name = "hot_score")
    private Double hotScore = 0.0;

    // ✅ NEW: Optimization for background scoring jobs
    @Column(name = "last_score_recalculation_at")
    private Instant lastScoreRecalculationAt;
}