package com.tuniv.backend.chat.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("MESSAGE") // ✅ This value is stored in the 'post_type' column
@Getter
@Setter
public class Message extends Post { // ✅ EXTENDS Post

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @JsonBackReference("conversation-messages")
    private Conversation conversation;
    
    // ✅ Override author setter to also set sender for consistency.
    @Override
    public void setAuthor(User author) {
        super.setAuthor(author);
        this.setSender(author);
    }
    
    // ✅ The 'sender' relationship is specific to Message, so we keep it.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    @JsonBackReference("user-messages")
    private User sender;
}