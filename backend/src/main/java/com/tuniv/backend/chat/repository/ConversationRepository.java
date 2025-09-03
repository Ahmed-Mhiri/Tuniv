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
    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.user.userId = :userId ORDER BY c.createdAt DESC")
    List<Conversation> findConversationsByUserId(@Param("userId") Integer userId);

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
}