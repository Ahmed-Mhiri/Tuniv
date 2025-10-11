package com.tuniv.backend.chat.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.Where;

import com.tuniv.backend.shared.model.Auditable;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.user.model.User;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_conversation_updated", columnList = "updated_at DESC"),
    @Index(name = "idx_conversation_university", columnList = "university_context_id"),
    @Index(name = "idx_conversation_created", columnList = "created_at DESC"),
    @Index(name = "idx_conversation_last_message", columnList = "last_message_sent_at DESC"),
    @Index(name = "idx_conversation_active", columnList = "is_active, last_message_sent_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@Where(clause = "is_active = true") // ✅ ADD: Global filter for active conversations

public class Conversation extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer conversationId;

    // ========== OPTIMISTIC LOCKING ==========
    @Version
    private Long version;

    // ========== BASIC INFO ==========
    @Size(max = 255)
    @Column(name = "title", length = 255)
    private String title;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_type", nullable = false)
    private ConversationType conversationType = ConversationType.DIRECT;

    // ========== CONTEXT ==========
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_context_id")
    private University universityContext;

    // ========== DENORMALIZED LAST MESSAGE INFO ==========
    @Size(max = 1000)
    @Column(name = "last_message_body", length = 1000)
    private String lastMessageBody;

    @Column(name = "last_message_sent_at")
    private Instant lastMessageSentAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_message_author_id")
    private User lastMessageAuthor;

    // ========== STATISTICS ==========
    @Column(name = "message_count", nullable = false)
    private Integer messageCount = 0;

    @Column(name = "participant_count", nullable = false)
    private Integer participantCount = 0;

    // ========== STATUS FLAGS ==========
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_archived", nullable = false)
    private boolean isArchived = false;
    
}