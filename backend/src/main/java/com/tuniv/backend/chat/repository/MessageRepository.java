package com.tuniv.backend.chat.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.chat.model.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {
    List<Message> findByConversationConversationIdOrderBySentAtAsc(Integer conversationId);
    Optional<Message> findTopByConversationConversationIdOrderBySentAtDesc(Integer conversationId);

    long countByConversationConversationIdAndSenderUserIdNotAndSentAtAfter(
            Integer conversationId,
            Integer userId,
            LocalDateTime timestamp
    );

}