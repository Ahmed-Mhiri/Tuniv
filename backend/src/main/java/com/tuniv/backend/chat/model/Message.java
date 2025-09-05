package com.tuniv.backend.chat.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.tuniv.backend.qa.model.Post;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("MESSAGE") // ✅ This value is stored in the 'post_type' column
@Getter
@Setter
public class Message extends Post { // ✅ EXTENDS Post

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt = Instant.now(); // ✅ Change to Instant



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @JsonBackReference("conversation-messages")
    private Conversation conversation;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;


    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MessageReaction> reactions = new HashSet<>();

    
}