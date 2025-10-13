package com.tuniv.backend.chat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.chat.model.DirectConversationLookup;

@Repository
public interface DirectConversationLookupRepository extends JpaRepository<DirectConversationLookup, Integer> {
    
    Optional<DirectConversationLookup> findByUser1IdAndUser2Id(Integer user1Id, Integer user2Id);
    
    @Query("SELECT dcl FROM DirectConversationLookup dcl WHERE " +
           "((dcl.user1Id = :user1Id AND dcl.user2Id = :user2Id) OR " +
           "(dcl.user1Id = :user2Id AND dcl.user2Id = :user1Id))")
    Optional<DirectConversationLookup> findConversationBetweenUsers(
            @Param("user1Id") Integer user1Id, 
            @Param("user2Id") Integer user2Id);
    
    boolean existsByConversation_ConversationId(Integer conversationId);
}