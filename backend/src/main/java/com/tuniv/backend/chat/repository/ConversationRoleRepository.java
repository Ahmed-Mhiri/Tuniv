package com.tuniv.backend.chat.repository;

import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRoleRepository extends JpaRepository<ConversationRole, Integer>, JpaSpecificationExecutor<ConversationRole> {

    // ========== Basic Finders ==========
    
    List<ConversationRole> findByConversation(Conversation conversation);
    
    List<ConversationRole> findByConversationIsNull(); // System roles
    
    Optional<ConversationRole> findByNameAndConversationIsNull(String name);
    
    Optional<ConversationRole> findByNameAndConversation(String name, Conversation conversation);
    
    List<ConversationRole> findByIsSystemRoleTrue();
    
    List<ConversationRole> findByIsDefaultTrue();
    
    Optional<ConversationRole> findByIsDefaultTrueAndConversation(Conversation conversation);

    // ========== Existence Checks ==========
    
    boolean existsByNameAndConversation(String name, Conversation conversation);
    
    boolean existsByNameAndConversationIsNull(String name);

    // ========== Update Operations ==========
    
    @Modifying
    @Query("UPDATE ConversationRole cr SET cr.memberCount = cr.memberCount + 1 WHERE cr.id = :roleId")
    void incrementMemberCount(@Param("roleId") Integer roleId);
    
    @Modifying
    @Query("UPDATE ConversationRole cr SET cr.memberCount = cr.memberCount - 1 WHERE cr.id = :roleId")
    void decrementMemberCount(@Param("roleId") Integer roleId);
    
    @Modifying
    @Query("UPDATE ConversationRole cr SET cr.memberCount = :count WHERE cr.id = :roleId")
    void setMemberCount(@Param("roleId") Integer roleId, @Param("count") Integer count);

    // ========== Statistics ==========
    
    @Query("SELECT COUNT(cr) FROM ConversationRole cr WHERE cr.conversation = :conversation")
    long countByConversation(@Param("conversation") Conversation conversation);
    
    @Query("SELECT SUM(cr.memberCount) FROM ConversationRole cr WHERE cr.conversation = :conversation")
    long sumMemberCountByConversation(@Param("conversation") Conversation conversation);
}