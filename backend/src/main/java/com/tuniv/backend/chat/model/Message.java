package com.tuniv.backend.chat.model;

import java.time.Instant;

import org.hibernate.annotations.Where;

import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_message_conversation_sentat", columnList = "conversation_id, sent_at DESC"),
    @Index(name = "idx_message_author", columnList = "user_id"),
    @Index(name = "idx_message_reply_to", columnList = "reply_to_message_id"),
    @Index(name = "idx_message_type", columnList = "message_type"),
    @Index(name = "idx_message_status", columnList = "is_deleted, sent_at DESC"),
    @Index(name = "idx_message_university_context", columnList = "university_context_id"), // ✅ KEEP THIS ONE
    @Index(name = "idx_message_author_conversation", columnList = "user_id, conversation_id")
})
@Getter
@Setter
@NoArgsConstructor
@Where(clause = "is_deleted = false")
public class Message extends Post {

    // ========== OPTIMISTIC LOCKING ==========
    @Version
    private Long version;

    // ========== MESSAGE-SPECIFIC FIELDS ==========
    @NotNull
    @Column(name = "sent_at", nullable = false)
    private Instant sentAt = Instant.now();

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    // ========== MESSAGE TYPE & CONTENT ==========
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType = MessageType.TEXT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private Message replyToMessage;

    /// Inside Message.java
    @Column(name = "edited_at")
    private Instant editedAt;

    // Inside Message.java
    @Column(name = "is_pinned", nullable = false)
    private boolean isPinned = false;

    // ✅ NEW: Added fields for pinning context
    @Column(name = "pinned_at")
    private Instant pinnedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pinned_by_user_id")
    private User pinnedBy;

    // ========== MESSAGE METADATA ==========
    @Column(name = "client_message_id")
    private String clientMessageId;

    @Override
    public String getPostType() {
        return "MESSAGE";
    }

    // ========== CONSTRUCTORS ==========
    public Message(String body, User author, Conversation conversation) {
        this.setBody(body);
        this.setAuthor(author);
        this.conversation = conversation;
        this.sentAt = Instant.now();

        if (conversation.getUniversityContext() != null) {
            this.setUniversityContext(conversation.getUniversityContext());
        }
    }

    public Message(String body, User author, Conversation conversation, Message replyTo) {
        this(body, author, conversation);
        this.replyToMessage = replyTo;
    }

    public Message(String body, User author, Conversation conversation, MessageType messageType) {
        this(body, author, conversation);
        this.messageType = messageType;
    }
}