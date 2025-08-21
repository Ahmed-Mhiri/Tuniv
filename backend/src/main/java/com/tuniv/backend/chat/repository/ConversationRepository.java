package com.tuniv.backend.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.chat.model.Conversation;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Integer> {
}