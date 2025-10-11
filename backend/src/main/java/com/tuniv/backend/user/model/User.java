package com.tuniv.backend.user.model;

import java.time.Instant;

import org.hibernate.annotations.Where;

import com.tuniv.backend.shared.model.Auditable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_reputation", columnList = "reputation_score DESC"),
    @Index(name = "idx_user_username", columnList = "username"),
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_created", columnList = "created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@Where(clause = "is_deleted = false")
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    // ========== OPTIMISTIC LOCKING ==========
    @Version
    private Long version;

    // ========== CORE PROFILE DATA ==========
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers and underscores")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Column(name = "username", unique = true, length = 50) // ✅ MODIFIED: nullable = false removed
    private String username;

    @NotNull
    @Email(message = "Email should be valid")
    @Column(name = "email", unique = true) // ✅ MODIFIED: nullable = false removed
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Size(max = 500)
    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Size(max = 100)
    @Column(name = "major", length = 100)
    private String major;

    @Size(max = 100)
    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "date_of_birth")
    private Instant dateOfBirth;

    // ========== REPUTATION & SCORES ==========
    @Column(name = "reputation_score", nullable = false)
    private Integer reputationScore = 0;

    @Column(name = "contribution_score", nullable = false)
    private Integer contributionScore = 0;

    // ========== ENHANCED DENORMALIZED COUNTERS ==========
    @Column(name = "topic_count", nullable = false)
    private int topicCount = 0;

    @Column(name = "reply_count", nullable = false)
    private int replyCount = 0;

    @Column(name = "solutions_count", nullable = false)
    private int solutionsCount = 0;

    @Column(name = "helpful_votes_received_count", nullable = false)
    private int helpfulVotesReceivedCount = 0;

    @Column(name = "follower_count", nullable = false)
    private int followerCount = 0;

    @Column(name = "following_count", nullable = false)
    private int followingCount = 0;

    // ========== AUTHENTICATION & VERIFICATION ==========
    @Column(name = "is_platform_admin", nullable = false)
    private boolean isPlatformAdmin = false;

    @Column(name = "is_moderator", nullable = false)
    private boolean isModerator = false;

    @Column(name = "is_2fa_enabled", nullable = false)
    private boolean is2faEnabled = false;

    @Column(name = "two_factor_auth_secret")
    private String twoFactorAuthSecret;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "is_email_verified", nullable = false)
    private boolean isEmailVerified = false;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled = true;

    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "reset_password_token_expiry")
    private Instant resetPasswordTokenExpiry;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    // ========== PRIVACY SETTINGS ==========
    @Enumerated(EnumType.STRING)
    @Column(name = "profile_visibility", nullable = false)
    private ProfileVisibility profileVisibility = ProfileVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_permissions", nullable = false)
    private MessagePermissions messagePermissions = MessagePermissions.ANYONE;

    @Column(name = "show_university_badges", nullable = false)
    private boolean showUniversityBadges = true;

    @Column(name = "show_online_status", nullable = false)
    private boolean showOnlineStatus = true;

    @Column(name = "allow_search_engine_indexing", nullable = false)
    private boolean allowSearchEngineIndexing = true;

    // ========== SOFT DELETE ENHANCEMENTS ==========
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deletion_reason")
    private String deletionReason;

    @Column(name = "original_username")
    private String originalUsername;

    @Column(name = "original_email")
    private String originalEmail;
    
    // ========== RELATIONSHIPS ==========
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private UserSettings settings;

    // ========== CONSTRUCTORS ==========
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.isEnabled = false;
    }
}