package com.tuniv.backend.chat.model;

import java.time.Instant;
import java.util.Set;

import com.tuniv.backend.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "conversations")
@Getter
@Setter
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer conversationId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "conversation")
    private Set<ConversationParticipant> participants;

    // ✅ NEW: Denormalized fields for performance
    @Column(name = "last_message_body", length = 1000)
    private String lastMessageBody;

    @Column(name = "last_message_sent_at")
    private Instant lastMessageSentAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_message_author_id")
    private User lastMessageAuthor;

    // ✅ NEW: Helper method to update last message
    public void updateLastMessage(Message message) {
        this.lastMessageBody = message.getBody();
        this.lastMessageSentAt = message.getSentAt();
        this.lastMessageAuthor = message.getAuthor();
    }
}