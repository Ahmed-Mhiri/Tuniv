package com.tuniv.backend.university.model;

import org.hibernate.annotations.Where;

import com.tuniv.backend.follow.model.Followable;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "universities", indexes = {
    @Index(name = "idx_university_name", columnList = "name"),
    @Index(name = "idx_university_domain", columnList = "email_domain"),
    @Index(name = "idx_university_member_count", columnList = "member_count DESC"),
    @Index(name = "idx_university_created", columnList = "created_at DESC"),
    @Index(name = "idx_university_active", columnList = "is_active, member_count DESC")
})
@Getter
@Setter
@NoArgsConstructor
@Where(clause = "is_active = true")
public class University extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer universityId;

    // ========== OPTIMISTIC LOCKING ==========
    @Version
    private Long version;

    // ========== BASIC INFO ==========
    @NotBlank(message = "University name cannot be empty")
    @Size(max = 100, message = "University name cannot exceed 100 characters")
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @NotBlank
    @Email
    @Column(name = "email_domain", nullable = false, unique = true)
    private String emailDomain;

    @Size(max = 2000)
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "website_url")
    private String websiteUrl;

    @Size(max = 100)
    @Column(name = "location", length = 100)
    private String location;

    // ========== CONFIGURATION ==========
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "join_policy", nullable = false)
    private UniversityJoinPolicy joinPolicy = UniversityJoinPolicy.REQUEST;

    @Column(name = "allow_cross_posting", nullable = false)
    private boolean allowCrossPosting = true;

    @Column(name = "allow_external_communities", nullable = false)
    private boolean allowExternalCommunities = true;

    @Column(name = "requires_verification_for_posting", nullable = false)
    private boolean requiresVerificationForPosting = true;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // ========== STATISTICS ==========
    @Column(name = "topic_count", nullable = false)
    private int topicCount = 0;

    @Column(name = "member_count", nullable = false)
    private int memberCount = 0;

    @Column(name = "community_count", nullable = false)
    private int communityCount = 0;

    @Column(name = "module_count", nullable = false)
    private int moduleCount = 0;

    @Column(name = "daily_active_members")
    private Integer dailyActiveMembers = 0;

    @Column(name = "weekly_new_members")
    private Integer weeklyNewMembers = 0;

      // ✅ ADD: Relationship to Followable
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false, orphanRemoval = true)
    @JoinColumn(name = "followable_id", nullable = false, unique = true)
    private Followable followable;
    
    // ========== CONSTRUCTORS ==========
    public University(String name, String emailDomain) {
        this.name = name;
        this.emailDomain = emailDomain;
        this.isActive = true;
    }

    public University(String name, String emailDomain, String location) {
        this(name, emailDomain);
        this.location = location;
    }
}