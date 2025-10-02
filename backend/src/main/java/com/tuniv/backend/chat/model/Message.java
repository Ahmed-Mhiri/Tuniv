package com.tuniv.backend.chat.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.tuniv.backend.qa.model.Post;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("MESSAGE")
@Table(name = "posts", indexes = {
    @Index(name = "idx_message_conversation_sentat", columnList = "conversation_id, sent_at DESC")
})
@Getter
@Setter
public class Message extends Post {

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @JsonBackReference("conversation-messages")
    private Conversation conversation;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    // ✅ NEW: Update conversation's last message when a message is sent
    @PostPersist
    @PostUpdate
    public void updateConversationLastMessage() {
        if (!this.isDeleted && this.conversation != null) {
            this.conversation.updateLastMessage(this);
        }
    }
}