package com.tuniv.backend.community.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import org.hibernate.annotations.Where;

import com.tuniv.backend.shared.model.Auditable;
import com.tuniv.backend.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "community_memberships", indexes = {
    @Index(name = "idx_community_membership_user", columnList = "user_id"),
    @Index(name = "idx_community_membership_community", columnList = "community_id"),
    @Index(name = "idx_community_membership_role", columnList = "role_id"),
    @Index(name = "idx_community_membership_active", columnList = "is_active, joined_at DESC"),
    @Index(name = "idx_community_membership_user_community", columnList = "user_id, community_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@Where(clause = "is_active = true")
public class CommunityMembership extends Auditable {

    @EmbeddedId
    private CommunityMembershipId id;

    // ========== CORE RELATIONSHIPS ==========
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("communityId")
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    // ========== ROLE RELATIONSHIP ==========
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private CommunityRole role;

    // ========== STATUS FLAGS ==========
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled = true;

    @Column(name = "is_banned", nullable = false)
    private boolean isBanned = false;

    // ========== METADATA ==========
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    @Column(name = "last_visited_at")
    private Instant lastVisitedAt;

    @Column(name = "visit_count", nullable = false)
    private Integer visitCount = 0;

    @Column(name = "contribution_score", nullable = false)
    private Integer contributionScore = 0;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    // ========== MODERATION FIELDS ==========
    @Column(name = "banned_at")
    private Instant bannedAt;

    @Column(name = "ban_reason")
    private String banReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_by_user_id")
    private User bannedBy;

    @Column(name = "mute_until")
    private Instant muteUntil;

    @Column(name = "mute_reason")
    private String muteReason;

    // ========== CONSTRUCTORS ==========
    public CommunityMembership(User user, Community community, CommunityRole role) {
        this.id = new CommunityMembershipId(user.getUserId(), community.getCommunityId());
        this.user = user;
        this.community = community;
        this.role = role;
    }

    // ========== EQUALS & HASHCODE ==========
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommunityMembership that = (CommunityMembership) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ========== EMBEDDED ID CLASS ==========
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommunityMembershipId implements Serializable {
        @Column(name = "user_id")
        private Integer userId;

        @Column(name = "community_id")
        private Integer communityId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CommunityMembershipId that = (CommunityMembershipId) o;
            return Objects.equals(userId, that.userId) && Objects.equals(communityId, that.communityId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, communityId);
        }
    }
}