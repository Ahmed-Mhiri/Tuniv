package com.tuniv.backend.chat.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import com.tuniv.backend.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "conversation_participants")
@Getter
@Setter
public class ConversationParticipant {

    @EmbeddedId
    private ConversationParticipantId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("conversationId")
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @Column(name = "last_read_timestamp")
    private Instant  lastReadTimestamp;

    /**
     * ✅ FIX: Default constructor required by JPA.
     */
    public ConversationParticipant() {
    }

    /**
     * ✅ FIX: Add a constructor to easily create new instances.
     * This constructor sets the user, the conversation, and correctly
     * initializes the composite primary key (id).
     */
    public ConversationParticipant(User user, Conversation conversation) {
        this.user = user;
        this.conversation = conversation;
        this.id = new ConversationParticipantId(user.getUserId(), conversation.getConversationId());
    }

    @Embeddable
    @Getter
    @Setter
    public static class ConversationParticipantId implements Serializable {
        @Column(name = "user_id")
        private Integer userId;
        
        @Column(name = "conversation_id")
        private Integer conversationId;

        public ConversationParticipantId() {
        }

        public ConversationParticipantId(Integer userId, Integer conversationId) {
            this.userId = userId;
            this.conversationId = conversationId;
        }

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