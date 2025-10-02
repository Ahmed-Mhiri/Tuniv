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

    // ✅ KEEP: This is efficient with the index
    @Query("""
    SELECT m FROM Message m
    LEFT JOIN FETCH m.author
    LEFT JOIN FETCH m.attachments
    WHERE m.conversation.conversationId = :conversationId
    ORDER BY m.sentAt ASC
    """)
    List<Message> findByConversation_ConversationIdOrderBySentAtAsc(@Param("conversationId") Integer conversationId);

    // ✅ KEEP: Simple and useful
    Optional<Message> findTopByConversation_ConversationIdOrderBySentAtDesc(Integer conversationId);

    // ✅ KEEP: This is fine for calculating unread counts
    long countByConversation_ConversationIdAndAuthor_UserIdNotAndSentAtAfter(
            Integer conversationId,
            Integer userId,
            Instant timestamp
    );

    // ❌ REMOVE: The batch unread count method - not needed yet
}