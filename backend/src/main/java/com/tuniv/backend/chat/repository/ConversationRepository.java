package com.tuniv.backend.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.chat.model.Conversation;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Integer> {
    
    // ✅ SIMPLE VERSION: Just get basic conversation data with denormalized fields
    @Query("""
    SELECT c FROM Conversation c
    JOIN c.participants cp 
    JOIN cp.user u
    WHERE cp.user.userId = :userId
    ORDER BY COALESCE(c.lastMessageSentAt, c.createdAt) DESC
    """)
    List<Conversation> findConversationsByUser(@Param("userId") Integer userId);

    // ✅ SIMPLE: Get conversation with participants (for sending messages)
    @Query("SELECT c FROM Conversation c JOIN FETCH c.participants p JOIN FETCH p.user WHERE c.conversationId = :id")
    Optional<Conversation> findByIdWithParticipantsAndUsers(@Param("id") Integer conversationId);

    // ✅ KEEP: This native query is actually efficient
    @Query(value = """
        SELECT c.* FROM conversations c
        WHERE (
            SELECT COUNT(cp.user_id)
            FROM conversation_participants cp
            WHERE cp.conversation_id = c.conversation_id
        ) = 2 AND EXISTS (
            SELECT 1 FROM conversation_participants cp
            WHERE cp.conversation_id = c.conversation_id AND cp.user_id = :userId1
        ) AND EXISTS (
            SELECT 1 FROM conversation_participants cp
            WHERE cp.conversation_id = c.conversation_id AND cp.user_id = :userId2
        )
    """, nativeQuery = true)
    Optional<Conversation> findDirectConversationBetweenUsers(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);

    // ❌ REMOVE: The complex summary queries - we'll handle this in service
}