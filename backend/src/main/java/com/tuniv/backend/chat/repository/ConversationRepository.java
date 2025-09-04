package com.tuniv.backend.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.chat.dto.ConversationSummaryDto;
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


    @Query("""
    SELECT new com.tuniv.backend.chat.dto.ConversationSummaryDto(
        c.conversationId,
        otherUser.userId,
        otherUser.username,
        otherUser.profilePhotoUrl,
        (SELECT m.body FROM Message m WHERE m.conversation = c ORDER BY m.sentAt DESC LIMIT 1),
        CAST(COALESCE((SELECT m2.sentAt FROM Message m2 WHERE m2.conversation = c ORDER BY m2.sentAt DESC LIMIT 1), c.createdAt) AS string),
        (SELECT COUNT(m3) FROM Message m3 WHERE m3.conversation = c AND m3.author.id != :currentUserId AND m3.sentAt > COALESCE(cp.lastReadTimestamp, c.createdAt))
    )
    FROM Conversation c
    JOIN c.participants cp ON cp.user.id = :currentUserId
    JOIN c.participants otherCp ON otherCp.conversation = c
    JOIN otherCp.user otherUser
    WHERE c = cp.conversation AND otherUser.id != :currentUserId
    ORDER BY COALESCE((SELECT m4.sentAt FROM Message m4 WHERE m4.conversation = c ORDER BY m4.sentAt DESC LIMIT 1), c.createdAt) DESC
""")
List<ConversationSummaryDto> findConversationSummariesForUser(@Param("currentUserId") Integer currentUserId);

@Query("SELECT c FROM Conversation c JOIN FETCH c.participants p JOIN FETCH p.user WHERE c.conversationId = :id")
    Optional<Conversation> findByIdWithParticipantsAndUsers(@Param("id") Integer conversationId);
}