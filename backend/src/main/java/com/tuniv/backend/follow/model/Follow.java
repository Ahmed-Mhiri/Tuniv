package com.tuniv.backend.follow.model;

import java.util.Objects;

import com.tuniv.backend.shared.model.Auditable;
import com.tuniv.backend.university.model.University;
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
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "follows", 
    indexes = {
        @Index(name = "idx_follows_user_followable", columnList = "user_id, followable_id", unique = true),
        @Index(name = "idx_follows_followable", columnList = "followable_id"),
        @Index(name = "idx_follows_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_follows_mutual", columnList = "is_mutual, created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Follow extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer followId;

    // ========== OPTIMISTIC LOCKING ==========
    @Version
    private Long version;

    // ========== FOLLOWER RELATIONSHIP ==========
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User follower;

    // ========== TARGET (Now using Followable) ==========
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followable_id", nullable = false)
    private Followable target;

    // ========== SETTINGS ==========
    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled = true;

    @Column(name = "is_mutual", nullable = false)
    private boolean isMutual = false;

    // ========== CONSTRUCTORS ==========
    public Follow(User follower, Followable target) {
        this.follower = follower;
        this.target = target;
    }
}