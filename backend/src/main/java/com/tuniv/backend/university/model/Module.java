package com.tuniv.backend.university.model;

import org.hibernate.annotations.Where;
import com.tuniv.backend.follow.model.Followable;
import com.tuniv.backend.shared.model.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "modules", indexes = {
    @Index(name = "idx_module_name", columnList = "name"),
    @Index(name = "idx_module_university", columnList = "university_id"),
    @Index(name = "idx_module_topic_count", columnList = "topic_count DESC"),
    @Index(name = "idx_module_code", columnList = "code"),
    @Index(name = "idx_module_active", columnList = "is_active, topic_count DESC")
})
@Getter
@Setter
@NoArgsConstructor
@Where(clause = "is_active = true")
public class Module extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer moduleId;

    // ========== OPTIMISTIC LOCKING ==========
    @Version
    private Long version;

    // ========== BASIC INFO ==========
    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 1000)
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Size(max = 20)
    @Column(name = "code", length = 20)
    private String code;

    // ========== STATISTICS ==========
    @Column(name = "topic_count", nullable = false)
    private int topicCount = 0;

    @Column(name = "follower_count", nullable = false)
    private int followerCount = 0;

    @Column(name = "active_participants")
    private Integer activeParticipants = 0;

    // ========== ACCESS CONTROL ==========
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "posting_policy", nullable = false)
    private ModulePostingPolicy postingPolicy = ModulePostingPolicy.UNIVERSITY_MEMBERS;

    @Column(name = "allow_external_posting", nullable = false)
    private boolean allowExternalPosting = true;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // ========== RELATIONSHIPS ==========
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    // ❌ REMOVED: Dangerous topics collection
    // @OneToMany(mappedBy = "module", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    // private Set<Topic> topics = new HashSet<>();

    // ========== FOLLOWABLE ==========
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false, orphanRemoval = true)
    @JoinColumn(name = "followable_id", nullable = false, unique = true)
    private Followable followable;
}