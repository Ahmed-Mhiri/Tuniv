package com.tuniv.backend.chat.repository;

import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Integer>, JpaSpecificationExecutor<Conversation> {

    // ========== Basic Finders ==========
    
    List<Conversation> findByConversationType(ConversationType conversationType);
    
    Page<Conversation> findByConversationType(ConversationType conversationType, Pageable pageable);
    
    List<Conversation> findByUniversityContext_UniversityId(Integer universityId);
    
    Page<Conversation> findByUniversityContext_UniversityId(Integer universityId, Pageable pageable);
    
    List<Conversation> findByIsActiveTrue();
    
    List<Conversation> findByIsArchivedTrue();
    
    List<Conversation> findByIsArchivedFalse();
    
    Page<Conversation> findByIsArchived(boolean isArchived, Pageable pageable);

    // ========== Complex Queries ==========
    
    @Query("SELECT c FROM Conversation c WHERE c.conversationType = 'DIRECT' AND " +
           "EXISTS (SELECT 1 FROM ConversationParticipant cp1 WHERE cp1.conversation = c AND cp1.user.userId = :user1Id) AND " +
           "EXISTS (SELECT 1 FROM ConversationParticipant cp2 WHERE cp2.conversation = c AND cp2.user.userId = :user2Id)")
    Optional<Conversation> findDirectConversationBetweenUsers(@Param("user1Id") Integer user1Id, 
                                                             @Param("user2Id") Integer user2Id);
    
    @Query("SELECT c FROM Conversation c WHERE c.title LIKE %:title%")
    Page<Conversation> findByTitleContaining(@Param("title") String title, Pageable pageable);
    
    @Query("SELECT c FROM Conversation c WHERE c.conversationType = 'GROUP' AND c.title LIKE %:query%")
    Page<Conversation> findGroupsByTitle(@Param("query") String query, Pageable pageable);

    // ========== User-Specific Queries ==========
    
    @Query("SELECT c FROM Conversation c JOIN c.participants cp WHERE cp.user.userId = :userId AND cp.isActive = true AND c.isActive = true")
    Page<Conversation> findActiveConversationsByUserId(@Param("userId") Integer userId, Pageable pageable);
    
    @Query("SELECT c FROM Conversation c JOIN c.participants cp WHERE cp.user.userId = :userId AND cp.isActive = true AND c.isActive = true AND c.isArchived = false")
    Page<Conversation> findActiveNonArchivedConversationsByUserId(@Param("userId") Integer userId, Pageable pageable);
    
    @Query("SELECT c FROM Conversation c JOIN c.participants cp WHERE cp.user.userId = :userId AND cp.isActive = true AND c.isActive = true AND c.isArchived = true")
    Page<Conversation> findArchivedConversationsByUserId(@Param("userId") Integer userId, Pageable pageable);
    
    @Query("SELECT COUNT(c) FROM Conversation c JOIN c.participants cp WHERE cp.user.userId = :userId AND cp.isActive = true AND c.isActive = true AND c.isArchived = false")
    long countActiveConversationsByUserId(@Param("userId") Integer userId);

    // ========== Statistics ==========
    
    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.universityContext.universityId = :universityId AND c.isActive = true")
    long countActiveConversationsByUniversity(@Param("universityId") Integer universityId);
    
    @Query("SELECT c FROM Conversation c WHERE c.messageCount > :minMessageCount ORDER BY c.messageCount DESC")
    Page<Conversation> findMostActiveConversations(@Param("minMessageCount") Integer minMessageCount, Pageable pageable);
    
    @Query("SELECT c FROM Conversation c WHERE c.participantCount > :minParticipants ORDER BY c.participantCount DESC")
    List<Conversation> findLargestConversations(@Param("minParticipants") Integer minParticipants, Pageable pageable);

    // ========== Search ==========
    
    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.title LIKE %:query% OR " +
           "EXISTS (SELECT 1 FROM ConversationParticipant cp JOIN cp.user u WHERE cp.conversation = c AND u.username LIKE %:query%)) " +
           "AND c.isActive = true")
    Page<Conversation> searchConversations(@Param("query") String query, Pageable pageable);

    @EntityGraph(attributePaths = {"participants", "participants.user", "participants.role"})
    Optional<Conversation> findByIdWithParticipants(Integer conversationId);
}