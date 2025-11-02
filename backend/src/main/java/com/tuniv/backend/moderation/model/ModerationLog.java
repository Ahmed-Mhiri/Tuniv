package com.tuniv.backend.moderation.model;

import java.time.Instant;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.CreationTimestamp;

import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.user.model.User;

import jakarta.persistence.Column;
import static jakarta.persistence.DiscriminatorType.STRING;
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

    // ❌ REMOVED this field:
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "target_post_id")
    // private Post targetPost;
    
    // ✅ KEPT this: Denormalized for fast history lookups
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser; // The user who was the subject of the action

    // ✅ ADDED THESE for polymorphism:
    @AnyDiscriminator(STRING)
    @AnyDiscriminatorValue(discriminator = "POST", entity = Post.class)
    @AnyDiscriminatorValue(discriminator = "USER", entity = User.class)
    @AnyDiscriminatorValue(discriminator = "COMMUNITY", entity = Community.class)
    @AnyDiscriminatorValue(discriminator = "MESSAGE", entity = Message.class)
    @Column(name = "target_type", length = 50)
    private String targetType; // The class name of the target (e.g., "POST")

    @Any
    @JoinColumn(name = "target_id")
    private Object target; // The actual entity being acted on (e.g., a Post object)

    @CreationTimestamp
    private Instant createdAt;
}