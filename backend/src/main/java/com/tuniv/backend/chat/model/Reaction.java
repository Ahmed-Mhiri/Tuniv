package com.tuniv.backend.chat.model;

import java.time.Instant;

import org.hibernate.annotations.Where;

import com.tuniv.backend.shared.model.Auditable;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reactions", indexes = {
    @Index(name = "idx_reaction_message_user", columnList = "message_id, user_id", unique = true),
    @Index(name = "idx_reaction_message", columnList = "message_id, created_at"),
    @Index(name = "idx_reaction_user", columnList = "user_id, created_at"),
    @Index(name = "idx_reaction_emoji", columnList = "emoji, created_at"),
    @Index(name = "idx_reaction_university", columnList = "university_context_id")
})
@Getter
@Setter
@NoArgsConstructor
@Where(clause = "is_removed = false") // ✅ ADD: Global filter for non-removed reactions
public class Reaction extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ========== OPTIMISTIC LOCKING ==========
    @Version
    private Long version;

    // ========== CORE RELATIONSHIPS ==========
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    // ========== REACTION DATA ==========
    @NotBlank
    @Column(name = "emoji", nullable = false, length = 10)
    private String emoji;

    @Column(name = "skin_tone")
    private Integer skinTone; // For emoji skin tone variations (1-5)

    @Size(max = 100)
    @Column(name = "custom_text", length = 100)
    private String customText; // For text-based reactions

    // ========== CONTEXT & METADATA ==========
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_context_id")
    private University universityContext;

    @Column(name = "is_removed", nullable = false)
    private boolean isRemoved = false;

    @Column(name = "removed_at")
    private Instant removedAt;

    @Column(name = "removal_reason")
    private String removalReason;
}