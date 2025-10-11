package com.tuniv.backend.qa.model;


import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Where;

import com.tuniv.backend.follow.model.Followable;
import com.tuniv.backend.shared.model.Auditable;
import com.tuniv.backend.university.model.University;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
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
@Table(name = "tags", indexes = {
    @Index(name = "idx_tag_name", columnList = "name"),
    @Index(name = "idx_tag_usage", columnList = "usage_count DESC"),
    @Index(name = "idx_tag_created", columnList = "created_at DESC"),
    @Index(name = "idx_tag_university", columnList = "university_id"),
    @Index(name = "idx_tag_official", columnList = "is_official, usage_count DESC")
})
@Getter
@Setter
@NoArgsConstructor
@Where(clause = "is_active = true")
public class Tag extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Integer id;

    // ========== OPTIMISTIC LOCKING ==========
    @Version
    private Long version;

    // ========== BASIC INFO ==========
    @NotBlank
    @Size(min = 2, max = 50)
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Size(max = 500)
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Size(min = 7, max = 7)
    @Column(name = "color", length = 7)
    private String color;

    // ========== STATUS FLAGS ==========
    @Column(name = "is_official", nullable = false)
    private boolean isOfficial = false;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // ========== STATISTICS ==========
    @NotNull
    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    @NotNull
    @Column(name = "follower_count", nullable = false)
    private Integer followerCount = 0;

    @Column(name = "weekly_usage_count")
    private Integer weeklyUsageCount = 0;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    // ========== CONTEXT ==========
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id")
    private University university;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false, orphanRemoval = true)
    @JoinColumn(name = "followable_id", nullable = false, unique = true)
    private Followable followable;
    

    // ❌ REMOVED: Collection - use repository queries instead
    // - Set<Topic> topics (replaced by TopicTag entity)

    // ========== CONSTRUCTORS ==========
    public Tag(String name, String description) {
        this.name = normalizeTagName(name);
        this.description = description;
    }

    public Tag(String name, String description, University university) {
        this(name, description);
        this.university = university;
    }

    public Tag(String name, String description, String color, boolean isOfficial) {
        this(name, description);
        this.color = color;
        this.isOfficial = isOfficial;
    }

    private String normalizeTagName(String name) {
        if (name == null) return null;
        return name.trim().toLowerCase().replaceAll("\\s+", "-");
    }
}