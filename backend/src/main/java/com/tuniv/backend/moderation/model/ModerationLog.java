package com.tuniv.backend.moderation.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "moderation_logs")
@Getter
@Setter
public class ModerationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer logId;
    
    // The moderator who took the action
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moderator_id", nullable = false)
    private User moderator;
    
    @Column(nullable = false)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String justification;

    // ✅ IMPROVED: Proper JPA relationships instead of raw IDs
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_post_id")
    private Post targetPost;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @CreationTimestamp
    private Instant createdAt;
}