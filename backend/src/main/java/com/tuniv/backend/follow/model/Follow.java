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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "follows", uniqueConstraints = {
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
}
