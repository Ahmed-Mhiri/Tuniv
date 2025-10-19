package com.tuniv.backend.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationType;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Integer>, JpaSpecificationExecutor<Conversation> {

    // ========== BASIC FINDERS ==========
    List<Conversation> findByConversationType(ConversationType conversationType);
    Page<Conversation> findByConversationType(ConversationType conversationType, Pageable pageable);
    List<Conversation> findByUniversityContext_UniversityId(Integer universityId);
    Page<Conversation> findByUniversityContext_UniversityId(Integer universityId, Pageable pageable);
    List<Conversation> findByIsActiveTrue();
    List<Conversation> findByIsArchivedTrue();
    List<Conversation> findByIsArchivedFalse();
    Page<Conversation> findByIsArchived(boolean isArchived, Pageable pageable);
    
    // ========== BULK OPERATIONS ==========
    List<Conversation> findByConversationIdIn(List<Integer> conversationIds);
    List<Conversation> findByConversationIdInAndIsActiveTrue(List<Integer> conversationIds);

    // ========== DIRECT CONVERSATION LOOKUP ==========
    @Query("SELECT c FROM Conversation c WHERE c.conversationType = 'DIRECT' AND " +
           "EXISTS (SELECT 1 FROM ConversationParticipant cp1 WHERE cp1.conversation = c AND cp1.user.userId = :user1Id) AND " +
           "EXISTS (SELECT 1 FROM ConversationParticipant cp2 WHERE cp2.conversation = c AND cp2.user.userId = :user2Id)")
    Optional<Conversation> findDirectConversationBetweenUsers(@Param("user1Id") Integer user1Id, 
                                                             @Param("user2Id") Integer user2Id);

    // ========== USER-SPECIFIC QUERIES ==========
    @Query("SELECT c FROM Conversation c JOIN c.participants cp WHERE cp.user.userId = :userId AND cp.isActive = true AND c.isActive = true")
    Page<Conversation> findActiveConversationsByUserId(@Param("userId") Integer userId, Pageable pageable);
    
    @Query("SELECT c FROM Conversation c JOIN c.participants cp WHERE cp.user.userId = :userId AND cp.isActive = true AND c.isActive = true AND c.isArchived = false")
    Page<Conversation> findActiveNonArchivedConversationsByUserId(@Param("userId") Integer userId, Pageable pageable);
    
    @Query("SELECT c FROM Conversation c JOIN c.participants cp WHERE cp.user.userId = :userId AND cp.isActive = true AND c.isActive = true AND c.isArchived = true")
    Page<Conversation> findArchivedConversationsByUserId(@Param("userId") Integer userId, Pageable pageable);

    // ========== SEARCH & DISCOVERY ==========
    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.title LIKE %:query% OR " +
           "EXISTS (SELECT 1 FROM ConversationParticipant cp JOIN cp.user u WHERE cp.conversation = c AND u.username LIKE %:query%)) " +
           "AND c.isActive = true")
    Page<Conversation> searchConversations(@Param("query") String query, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.conversationType = 'GROUP' AND c.title LIKE %:query%")
    Page<Conversation> findGroupsByTitle(@Param("query") String query, Pageable pageable);

    // ========== STATISTICS & ANALYTICS ==========
    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.universityContext.universityId = :universityId AND c.isActive = true")
    long countActiveConversationsByUniversity(@Param("universityId") Integer universityId);
    
    @Query("SELECT COUNT(c) FROM Conversation c JOIN c.participants cp WHERE cp.user.userId = :userId AND cp.isActive = true AND c.isActive = true AND c.isArchived = false")
    long countActiveConversationsByUserId(@Param("userId") Integer userId);
    
    @Query("SELECT c FROM Conversation c WHERE c.messageCount > :minMessageCount ORDER BY c.messageCount DESC")
    Page<Conversation> findMostActiveConversations(@Param("minMessageCount") Integer minMessageCount, Pageable pageable);
    
    @Query("SELECT c FROM Conversation c WHERE c.participantCount > :minParticipants ORDER BY c.participantCount DESC")
    List<Conversation> findLargestConversations(@Param("minParticipants") Integer minParticipants, Pageable pageable);

    // ========== ONLINE STATUS QUERIES ==========
    @Query("SELECT c FROM Conversation c WHERE c.onlineParticipantCount > 0 ORDER BY c.lastActivityAt DESC")
    Page<Conversation> findActiveConversationsWithOnlineUsers(Pageable pageable);
    
    @Query("SELECT c FROM Conversation c WHERE c.recentActiveParticipantCount > :threshold AND c.isLargeGroup = true")
    List<Conversation> findLargeGroupsWithRecentActivity(@Param("threshold") Integer threshold);

    // ========== ENTITY GRAPH METHODS ==========
    @EntityGraph(attributePaths = {"participants", "participants.user", "participants.role"})
    Optional<Conversation> findWithParticipantsById(Integer conversationId);

    @EntityGraph(attributePaths = {"participants.user", "participants.role", "universityContext"})
    Optional<Conversation> findWithParticipantsAndContextById(Integer conversationId);

    @EntityGraph(attributePaths = {"lastMessageAuthor", "universityContext"})
    Optional<Conversation> findWithLastMessageAuthorById(Integer conversationId);

    // ========== EXISTENCE CHECKS ==========
    boolean existsByConversationId(Integer conversationId);
    
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Conversation c WHERE c.conversationId = :conversationId AND c.isActive = true")
    boolean existsActiveByConversationId(@Param("conversationId") Integer conversationId);
}