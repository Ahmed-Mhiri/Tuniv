package com.tuniv.backend.chat.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import org.hibernate.annotations.Where;

import com.tuniv.backend.shared.model.Auditable;
import com.tuniv.backend.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conversation_participants", indexes = {
    @Index(name = "idx_conv_part_user", columnList = "user_id"),
    @Index(name = "idx_conv_part_conversation", columnList = "conversation_id"),
    @Index(name = "idx_conv_part_last_read", columnList = "last_read_timestamp"),
    @Index(name = "idx_conv_part_unread", columnList = "unread_count, last_read_timestamp"),
    @Index(name = "idx_conv_part_active", columnList = "is_active, joined_at")
})
@Getter
@Setter
@NoArgsConstructor
@Where(clause = "is_active = true") // ✅ ADD: Global filter for active participants
public class ConversationParticipant extends Auditable {

    @EmbeddedId
    private ConversationParticipantId id;

    // ========== CORE RELATIONSHIPS ==========
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    // Inside ConversationParticipant.java
    @Size(max = 50)
    @Column(name = "nickname", length = 50)
    private String nickname;


    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("conversationId")
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    // ========== READ STATUS ==========
    @Column(name = "last_read_timestamp")
    private Instant lastReadTimestamp;

    @Column(name = "unread_count", nullable = false)
    private Integer unreadCount = 0;

    // ========== PARTICIPANT STATUS ==========
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled = true;

    @Column(name = "is_muted", nullable = false)
    private boolean isMuted = false;

    @Column(name = "muted_until")
    private Instant mutedUntil;

    // ========== METADATA ==========
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @Column(name = "message_count", nullable = false)
    private Integer messageCount = 0;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private ConversationRole role;



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationParticipant that = (ConversationParticipant) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ========== EMBEDDED ID CLASS ==========
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationParticipantId implements Serializable {
        
        @Column(name = "user_id")
        private Integer userId;

        @Column(name = "conversation_id")
        private Integer conversationId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConversationParticipantId that = (ConversationParticipantId) o;
            return Objects.equals(userId, that.userId) && Objects.equals(conversationId, that.conversationId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, conversationId);
        }
    }
}