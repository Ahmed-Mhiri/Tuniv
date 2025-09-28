package com.tuniv.backend.user.model;

import java.time.OffsetDateTime; // <-- Use OffsetDateTime for time zone awareness
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.university.model.UniversityMembership;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id") // Explicit mapping
    private Integer userId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    @JsonIgnore
    private String password;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Column(name = "bio")
    private String bio;

    @Column(name = "major")
    private String major;

    @Column(name = "reputation_score", nullable = false)
    private Integer reputationScore = 0;
    
    // --- RELATIONSHIPS ---
    @OneToMany(mappedBy = "user")
    @JsonManagedReference("user-memberships")
    private Set<UniversityMembership> memberships = new HashSet<>();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Post> posts = new HashSet<>();

    // --- AUTHENTICATION FIELDS ---
    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "reset_password_token_expiry")
    private OffsetDateTime resetPasswordTokenExpiry; // Use OffsetDateTime

    @Column(name = "is_platform_admin", nullable = false)
    private boolean isPlatformAdmin = false;

    @Column(name = "is_2fa_enabled", nullable = false)
    private boolean is2faEnabled = false;

    @Column(name = "two_factor_auth_secret")
    @JsonIgnore
    private String twoFactorAuthSecret;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled = false; // Start as false
}