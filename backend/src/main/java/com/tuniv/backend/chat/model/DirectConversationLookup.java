package com.tuniv.backend.chat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "direct_conversation_lookup", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user1_id", "user2_id"}),
    @UniqueConstraint(columnNames = {"conversation_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class DirectConversationLookup {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "user1_id", nullable = false)
    private Integer user1Id;
    
    @Column(name = "user2_id", nullable = false)
    private Integer user2Id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    
    public DirectConversationLookup(Integer user1Id, Integer user2Id, Conversation conversation) {
        // Ensure consistent ordering
        if (user1Id < user2Id) {
            this.user1Id = user1Id;
            this.user2Id = user2Id;
        } else {
            this.user1Id = user2Id;
            this.user2Id = user1Id;
        }
        this.conversation = conversation;
    }
}