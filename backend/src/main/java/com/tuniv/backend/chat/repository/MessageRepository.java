package com.tuniv.backend.chat.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.chat.model.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {

    /**
     * ✅ RENAMED: Added underscore for nested property traversal (Conversation -> conversationId).
     * The @Query ensures author and attachments are fetched efficiently to prevent N+1 problems.
     */
    @Query("""
    SELECT m FROM Message m
    LEFT JOIN FETCH m.author
    LEFT JOIN FETCH m.attachments
    WHERE m.conversation.conversationId = :conversationId
    ORDER BY m.sentAt ASC
    """)
    List<Message> findByConversation_ConversationIdOrderBySentAtAsc(@Param("conversationId") Integer conversationId);

    /**
     * ✅ RENAMED: Added underscore for nested property traversal.
     */
    Optional<Message> findTopByConversation_ConversationIdOrderBySentAtDesc(Integer conversationId);

    /**
     * ✅ RENAMED & UPDATED: Added underscores for nested properties and changed parameter type
     * from LocalDateTime to Instant to match the entity fields.
     */
    long countByConversation_ConversationIdAndAuthor_UserIdNotAndSentAtAfter(
            Integer conversationId,
            Integer userId,
            Instant timestamp
    );
}