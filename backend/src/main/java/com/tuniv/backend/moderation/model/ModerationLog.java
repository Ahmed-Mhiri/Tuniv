package com.tuniv.backend.moderation.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

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
    private String action; // e.g., "REMOVED_POST", "MUTED_USER", "BANNED_USER"

    @Column(columnDefinition = "TEXT")
    private String justification; // Moderator's notes

    // Can be null if action is not on a specific post (e.g., banning a user)
    @Column(name = "target_post_id")
    private Integer targetPostId; 
    
    // Can be null if action is not on a specific user
    @Column(name = "target_user_id")
    private Integer targetUserId; 

    @CreationTimestamp
    private Instant createdAt;
}