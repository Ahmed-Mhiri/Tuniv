package com.tuniv.backend.follow.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.tuniv.backend.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "follows", 
    indexes = {
        @Index(name = "idx_follows_target", columnList = "target_type, target_id"), // ✅ NEW: For finding followers of a target
        @Index(name = "idx_follows_user_target", columnList = "user_id, target_type, target_id"), // ✅ NEW: For checking specific follow relationships
        @Index(name = "idx_follows_user_created", columnList = "user_id, created_at") // ✅ NEW: For user activity feeds
    },
    uniqueConstraints = {
        // A user can only follow a specific target once
        @UniqueConstraint(columnNames = {"user_id", "target_type", "target_id"})
    })
@Getter
@Setter
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer followId;

    // The user performing the follow action
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // What is being followed (e.g., 'USER', 'COMMUNITY')
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private FollowableType targetType;

    // The ID of the thing being followed (e.g., the userId, communityId)
    @Column(name = "target_id", nullable = false)
    private Integer targetId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // No-args constructor for JPA
    public Follow() {}

    // Convenience constructor
    public Follow(User user, FollowableType targetType, Integer targetId) {
        this.user = user;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    // ✅ NEW: Helper method to get target identifier
    public String getTargetIdentifier() {
        return targetType + ":" + targetId;
    }

    // ✅ NEW: Check if this follow is for a specific target
    public boolean isForTarget(FollowableType type, Integer id) {
        return this.targetType == type && this.targetId.equals(id);
    }

    // ✅ NEW: Check if this follow is by a specific user
    public boolean isByUser(Integer userId) {
        return this.user != null && this.user.getUserId().equals(userId);
    }

    // ✅ NEW: Override toString for better logging
    @Override
    public String toString() {
        return "Follow{" +
                "followId=" + followId +
                ", userId=" + (user != null ? user.getUserId() : "null") +
                ", targetType=" + targetType +
                ", targetId=" + targetId +
                ", createdAt=" + createdAt +
                '}';
    }

    // ✅ NEW: Override equals and hashCode for proper entity management
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Follow follow = (Follow) o;
        return followId != null && followId.equals(follow.followId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}