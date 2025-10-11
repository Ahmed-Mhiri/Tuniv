package com.tuniv.backend.community.model;

import org.hibernate.annotations.Where;

import com.tuniv.backend.follow.model.Followable;
import com.tuniv.backend.shared.model.Auditable;
import com.tuniv.backend.university.model.University;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "communities", indexes = {
    @Index(name = "idx_community_name", columnList = "name"),
    @Index(name = "idx_community_university", columnList = "university_id"), // ✅ ADDED: University relationship
    @Index(name = "idx_community_member_count", columnList = "member_count DESC"),
    @Index(name = "idx_community_topic_count", columnList = "topic_count DESC"),
    @Index(name = "idx_community_created", columnList = "created_at DESC"),
    @Index(name = "idx_community_active", columnList = "is_active, member_count DESC")
})
@Getter
@Setter
@NoArgsConstructor
@Where(clause = "is_active = true")
public class Community extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer communityId;

    // ========== OPTIMISTIC LOCKING ==========
    @Version
    private Long version;

    // ========== BASIC INFO ==========
    @NotBlank(message = "Community name cannot be empty")
    @Size(max = 100, message = "Community name cannot exceed 100 characters")
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Size(max = 2000)
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(name = "icon_url")
    private String iconUrl;

    // ========== UNIVERSITY CONTEXT ==========
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id") // ✅ ADDED: Link to university
    private University university;

    // ========== CONFIGURATION ==========
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "join_policy", nullable = false)
    private CommunityJoinPolicy joinPolicy = CommunityJoinPolicy.OPEN;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "posting_policy", nullable = false)
    private CommunityPostingPolicy postingPolicy = CommunityPostingPolicy.MEMBERS;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private CommunityVisibility visibility = CommunityVisibility.PUBLIC;

    @Column(name = "allow_external_members", nullable = false)
    private boolean allowExternalMembers = true;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    // ========== STATISTICS ==========
    @Column(name = "member_count", nullable = false)
    private int memberCount = 0;

    @Column(name = "topic_count", nullable = false)
    private int topicCount = 0;

    @Column(name = "follower_count", nullable = false)
    private int followerCount = 0;

    @Column(name = "daily_active_members")
    private Integer dailyActiveMembers = 0;

    @Column(name = "weekly_growth_rate")
    private Double weeklyGrowthRate = 0.0;

    // ========== FOLLOWABLE ==========
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false, orphanRemoval = true)
    @JoinColumn(name = "followable_id", nullable = false, unique = true)
    private Followable followable;
}