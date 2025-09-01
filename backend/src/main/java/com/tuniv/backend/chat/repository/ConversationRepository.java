package com.tuniv.backend.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.chat.model.Conversation;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Integer> {
    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.user.userId = :userId ORDER BY c.createdAt DESC")
    List<Conversation> findConversationsByUserId(@Param("userId") Integer userId);
}