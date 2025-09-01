package com.tuniv.backend.chat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tuniv.backend.chat.model.ConversationParticipant;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, ConversationParticipant.ConversationParticipantId> {

    // Method to find a specific participant entry
    Optional<ConversationParticipant> findByUserUserIdAndConversationConversationId(Integer userId, Integer conversationId);
}
