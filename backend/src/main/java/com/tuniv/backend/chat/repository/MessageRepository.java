package com.tuniv.backend.chat.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.chat.model.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {

    @Query("""
    SELECT m FROM Message m
    LEFT JOIN FETCH m.author
    LEFT JOIN FETCH m.attachments
    WHERE m.conversation.conversationId = :conversationId
    ORDER BY m.sentAt ASC
    """)
    List<Message> findByConversationConversationIdOrderBySentAtAsc(@Param("conversationId") Integer conversationId);

    Optional<Message> findTopByConversationConversationIdOrderBySentAtDesc(Integer conversationId);

    // ✅ RENAMED: Changed from 'SenderUserId' to 'AuthorUserId' to match the refactored Message entity.
    long countByConversationConversationIdAndAuthorUserIdNotAndSentAtAfter(
            Integer conversationId,
            Integer userId,
            LocalDateTime timestamp
    );
}