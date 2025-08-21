package com.tuniv.backend.chat.model;

import com.tuniv.backend.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "conversation_participants")
@Getter
@Setter
public class ConversationParticipant {

    @EmbeddedId
    private ConversationParticipantId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("conversationId")
    private Conversation conversation;

    @Embeddable
    @Getter
    @Setter
    public static class ConversationParticipantId implements Serializable {
        private Integer userId;
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