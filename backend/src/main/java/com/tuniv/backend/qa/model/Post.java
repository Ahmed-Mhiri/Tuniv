package com.tuniv.backend.qa.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Where;
import com.tuniv.backend.shared.model.Auditable;
import com.tuniv.backend.shared.model.SoftDeletable;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "posts", indexes = {
    @Index(name = "idx_posts_author_created", columnList = "user_id, created_at"),
    @Index(name = "idx_posts_university_context", columnList = "university_context_id"),
    @Index(name = "idx_posts_visibility", columnList = "visibility"),
    @Index(name = "idx_posts_created_at", columnList = "created_at DESC"),
    @Index(name = "idx_posts_deleted", columnList = "is_deleted, created_at")
})
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@Where(clause = "is_deleted = false")
public abstract class Post extends Auditable implements SoftDeletable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ========== OPTIMISTIC LOCKING ==========
    @Version
    private Long version;

    // ========== CORE CONTENT ==========
    @NotBlank(message = "Post body cannot be empty")
    @Size(max = 10000, message = "Post body cannot exceed 10000 characters")
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    // ========== CONTEXT & VISIBILITY ==========
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_context_id")
    private University universityContext;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private ContentVisibility visibility = ContentVisibility.PUBLIC;

    // ========== EDIT TRACKING ==========
    @Column(name = "is_edited", nullable = false)
    private boolean isEdited = false;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "edit_count", nullable = false)
    private Integer editCount = 0;

    @Column(name = "edit_history", columnDefinition = "TEXT")
    private String editHistory;

    // ========== SOFT DELETE FIELDS ==========
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deletion_reason")
    private String deletionReason;

    // ❌ REMOVED: Dangerous attachments collection
    // @OneToMany(mappedBy = "post", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    // private Set<Attachment> attachments = new HashSet<>();

    public abstract String getPostType();
}