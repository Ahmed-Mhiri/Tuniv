package com.tuniv.backend.chat.repository;

import java.time.Instant;
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
import com.tuniv.backend.chat.projection.conversation.ConversationDetailProjection;
import com.tuniv.backend.chat.projection.conversation.ConversationListProjection;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Integer>, JpaSpecificationExecutor<Conversation> {

    // ========== DTO PROJECTIONS FOR HIGH-TRAFFIC ENDPOINTS ==========
    
    // ✅ OPTIMIZED: Use projection for conversation lists
    @Query("SELECT c.conversationId as conversationId, c.title as title, c.conversationType as conversationType, " +
           "c.lastMessageBody as lastMessageBody, c.lastMessageSentAt as lastMessageSentAt, " +
           "c.lastMessageAuthor.userId as lastMessageAuthorId, c.participantCount as participantCount, " +
           "c.isArchived as isArchived, c.updatedAt as updatedAt " +
           "FROM Conversation c WHERE c.isActive = true")
    Page<ConversationListProjection> findActiveConversationsProjection(Pageable pageable);
    
    // ✅ OPTIMIZED: User-specific conversations with projection
    @Query("SELECT c.conversationId as conversationId, c.title as title, c.conversationType as conversationType, " +
           "c.lastMessageBody as lastMessageBody, c.lastMessageSentAt as lastMessageSentAt, " +
           "c.lastMessageAuthor.userId as lastMessageAuthorId, cp.unreadCount as unreadCount, " +
           "c.participantCount as participantCount, c.isArchived as isArchived, c.updatedAt as updatedAt " +
           "FROM Conversation c JOIN c.participants cp " +
           "WHERE cp.user.userId = :userId AND cp.isActive = true AND c.isActive = true AND c.isArchived = false")
    Page<ConversationListProjection> findActiveNonArchivedConversationsByUserIdProjection(
            @Param("userId") Integer userId, Pageable pageable);

    // ✅ NEW: Search conversations with projection
    @Query("SELECT c.conversationId as conversationId, c.title as title, c.conversationType as conversationType, " +
           "c.lastMessageBody as lastMessageBody, c.lastMessageSentAt as lastMessageSentAt, " +
           "c.lastMessageAuthor.userId as lastMessageAuthorId, cp.unreadCount as unreadCount, " +
           "c.participantCount as participantCount, c.isArchived as isArchived, c.updatedAt as updatedAt " +
           "FROM Conversation c JOIN c.participants cp " +
           "WHERE cp.user.userId = :userId AND cp.isActive = true AND c.isActive = true " + // User must be active participant
           "AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) OR EXISTS (" + // Title match OR
           "    SELECT 1 FROM ConversationParticipant cp2 JOIN cp2.user u " + // Participant username match
           "    WHERE cp2.conversation = c AND cp2.isActive = true AND LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))" +
           "))")
    Page<ConversationListProjection> searchConversationsProjection(
            @Param("query") String query, @Param("userId") Integer userId, Pageable pageable);

    // ✅ NEW: Archived conversations with projection
    @Query("SELECT c.conversationId as conversationId, c.title as title, c.conversationType as conversationType, " +
           "c.lastMessageBody as lastMessageBody, c.lastMessageSentAt as lastMessageSentAt, " +
           "c.lastMessageAuthor.userId as lastMessageAuthorId, cp.unreadCount as unreadCount, " +
           "c.participantCount as participantCount, c.isArchived as isArchived, c.updatedAt as updatedAt " +
           "FROM Conversation c JOIN c.participants cp " +
           "WHERE cp.user.userId = :userId AND cp.isActive = true AND c.isActive = true AND c.isArchived = true") // Filter for archived
    Page<ConversationListProjection> findArchivedConversationsByUserIdProjection(
            @Param("userId") Integer userId, Pageable pageable);

    // ✅ OPTIMIZED: Conversation details with projection
    @Query("SELECT c.conversationId as conversationId, c.title as title, c.conversationType as conversationType, " +
           "c.universityContext.universityId as universityContextId, c.lastMessageBody as lastMessageBody, " +
           "c.lastMessageSentAt as lastMessageSentAt, c.lastMessageAuthor.userId as lastMessageAuthorId, " +
           "c.messageCount as messageCount, c.participantCount as participantCount, c.isActive as isActive, " +
           "c.isArchived as isArchived, c.createdAt as createdAt, c.updatedAt as updatedAt, " +
           "c.onlineParticipantCount as onlineParticipantCount, c.recentlyActiveParticipantCount as recentlyActiveParticipantCount, " +
           "c.lastActivityAt as lastActivityAt, c.isLargeGroup as isLargeGroup " +
           "FROM Conversation c WHERE c.conversationId = :conversationId AND c.isActive = true")
    Optional<ConversationDetailProjection> findActiveConversationDetailById(@Param("conversationId") Integer conversationId);

    // ========== ENTITY GRAPH METHODS FOR EAGER LOADING ==========
    
    // ✅ OPTIMIZED: Eagerly load participants and their basic user info
    @EntityGraph(attributePaths = {
        "participants", 
        "participants.user",
        "participants.role"
    })
    @Query("SELECT c FROM Conversation c WHERE c.conversationId = :conversationId AND c.isActive = true")
    Optional<Conversation> findActiveWithParticipantsById(@Param("conversationId") Integer conversationId);
    
    // ✅ OPTIMIZED: For conversation details - load only necessary associations
    @EntityGraph(attributePaths = {
        "participants.user", 
        "participants.role", 
        "universityContext",
        "lastMessageAuthor"
    })
    @Query("SELECT c FROM Conversation c WHERE c.conversationId = :conversationId AND c.isActive = true")
    Optional<Conversation> findActiveWithDetailsById(@Param("conversationId") Integer conversationId);
    
    // ✅ OPTIMIZED: For message lists - minimal data
    @EntityGraph(attributePaths = {"lastMessageAuthor", "universityContext"})
    @Query("SELECT c FROM Conversation c WHERE c.conversationId = :conversationId AND c.isActive = true")
    Optional<Conversation> findActiveWithLastMessageAuthorById(@Param("conversationId") Integer conversationId);

    // ========== BASIC FINDERS ==========
    
    // @deprecated - Use findByConversationTypeAndIsActiveTrue instead
    @Deprecated
    List<Conversation> findByConversationType(ConversationType conversationType);
    
    // @deprecated - Use findByConversationTypeAndIsActiveTrue with Pageable instead
    @Deprecated
    Page<Conversation> findByConversationType(ConversationType conversationType, Pageable pageable);
    
    // ✅ OPTIMIZED: Use JOIN FETCH for active conversations by type
    @Query("SELECT c FROM Conversation c WHERE c.conversationType = :conversationType AND c.isActive = true")
    List<Conversation> findByConversationTypeAndIsActiveTrue(@Param("conversationType") ConversationType conversationType);
    
    // ✅ OPTIMIZED: Paginated with basic fields only
    @Query("SELECT c FROM Conversation c WHERE c.conversationType = :conversationType AND c.isActive = true")
    Page<Conversation> findByConversationTypeAndIsActiveTrue(@Param("conversationType") ConversationType conversationType, Pageable pageable);
    
    // @deprecated - Use findByUniversityContext_UniversityIdAndIsActiveTrue instead
    @Deprecated
    List<Conversation> findByUniversityContext_UniversityId(Integer universityId);
    
    // @deprecated - Use findByUniversityContext_UniversityIdAndIsActiveTrue with Pageable instead
    @Deprecated
    Page<Conversation> findByUniversityContext_UniversityId(Integer universityId, Pageable pageable);
    
    // ✅ ACTIVE-AWARE: Active conversations by university
    List<Conversation> findByUniversityContext_UniversityIdAndIsActiveTrue(Integer universityId);
    Page<Conversation> findByUniversityContext_UniversityIdAndIsActiveTrue(Integer universityId, Pageable pageable);
    
    // ✅ GOOD: Explicit active condition
    List<Conversation> findByIsActiveTrue();
    
    // @deprecated - Use findByIsArchivedTrueAndIsActiveTrue instead
    @Deprecated
    List<Conversation> findByIsArchivedTrue();
    
    // @deprecated - Use findByIsArchivedFalseAndIsActiveTrue instead
    @Deprecated
    List<Conversation> findByIsArchivedFalse();
    
    // @deprecated - Use findByIsArchivedAndIsActiveTrue instead
    @Deprecated
    Page<Conversation> findByIsArchived(boolean isArchived, Pageable pageable);
    
    // ✅ ACTIVE-AWARE: Archive status with active filter
    List<Conversation> findByIsArchivedTrueAndIsActiveTrue();
    List<Conversation> findByIsArchivedFalseAndIsActiveTrue();
    Page<Conversation> findByIsArchivedAndIsActiveTrue(boolean isArchived, Pageable pageable);
    
    // ========== BULK OPERATIONS ==========
    
    // @deprecated - Use findActiveByConversationIdIn instead
    @Deprecated
    List<Conversation> findByConversationIdIn(List<Integer> conversationIds);
    
    // ✅ GOOD: Explicit active condition
    List<Conversation> findByConversationIdInAndIsActiveTrue(List<Integer> conversationIds);
    
    // ✅ ACTIVE-AWARE: Bulk operations with active filter
    @Query("SELECT c FROM Conversation c WHERE c.conversationId IN :conversationIds AND c.isActive = true")
    List<Conversation> findActiveByConversationIdIn(@Param("conversationIds") List<Integer> conversationIds);

    // ✅ OPTIMIZED: Bulk fetch with JOIN FETCH
    @Query("SELECT DISTINCT c FROM Conversation c JOIN FETCH c.participants WHERE c.conversationId IN :conversationIds AND c.isActive = true")
    List<Conversation> findActiveByConversationIdInWithParticipants(@Param("conversationIds") List<Integer> conversationIds);

    // ========== DIRECT CONVERSATION LOOKUP ==========
    
    // @deprecated - Use findActiveDirectConversationBetweenUsers instead
    @Deprecated
    @Query("SELECT c FROM Conversation c WHERE c.conversationType = 'DIRECT' AND " +
           "EXISTS (SELECT 1 FROM ConversationParticipant cp1 WHERE cp1.conversation = c AND cp1.user.userId = :user1Id) AND " +
           "EXISTS (SELECT 1 FROM ConversationParticipant cp2 WHERE cp2.conversation = c AND cp2.user.userId = :user2Id)")
    Optional<Conversation> findDirectConversationBetweenUsers(@Param("user1Id") Integer user1Id, 
                                                             @Param("user2Id") Integer user2Id);

    // ✅ OPTIMIZED: Use JOIN FETCH for direct conversation lookup
    @Query("SELECT c FROM Conversation c JOIN FETCH c.participants cp1 JOIN FETCH cp1.user JOIN FETCH c.participants cp2 JOIN FETCH cp2.user " +
           "WHERE c.conversationType = 'DIRECT' AND c.isActive = true AND " +
           "cp1.user.userId = :user1Id AND cp1.isActive = true AND " +
           "cp2.user.userId = :user2Id AND cp2.isActive = true")
    Optional<Conversation> findActiveDirectConversationBetweenUsers(@Param("user1Id") Integer user1Id, 
                                                                   @Param("user2Id") Integer user2Id);

    // ========== USER-SPECIFIC QUERIES ==========
    
    // ✅ OPTIMIZED: Use JOIN FETCH to avoid N+1 for participant data
    @Query("SELECT DISTINCT c FROM Conversation c JOIN FETCH c.participants cp WHERE cp.user.userId = :userId AND cp.isActive = true AND c.isActive = true")
    Page<Conversation> findActiveConversationsByUserId(@Param("userId") Integer userId, Pageable pageable);
    
    // ✅ OPTIMIZED: Non-archived with JOIN FETCH
    @Query("SELECT DISTINCT c FROM Conversation c JOIN FETCH c.participants cp WHERE cp.user.userId = :userId AND cp.isActive = true AND c.isActive = true AND c.isArchived = false")
    Page<Conversation> findActiveNonArchivedConversationsByUserId(@Param("userId") Integer userId, Pageable pageable);
    
    // ✅ GOOD: Explicit active conditions with archive filter
    @Query("SELECT c FROM Conversation c JOIN c.participants cp WHERE cp.user.userId = :userId AND cp.isActive = true AND c.isActive = true AND c.isArchived = true")
    Page<Conversation> findArchivedConversationsByUserId(@Param("userId") Integer userId, Pageable pageable);

    // ========== SEARCH & DISCOVERY ==========
    
    // ✅ OPTIMIZED: Search with JOIN FETCH for basic participant data
    @Query("SELECT DISTINCT c FROM Conversation c LEFT JOIN FETCH c.participants cp LEFT JOIN FETCH cp.user " +
           "WHERE (c.title LIKE %:query% OR EXISTS (SELECT 1 FROM ConversationParticipant cp2 JOIN cp2.user u WHERE cp2.conversation = c AND u.username LIKE %:query%)) " +
           "AND c.isActive = true")
    Page<Conversation> searchConversations(@Param("query") String query, Pageable pageable);

    // @deprecated - Use findActiveGroupsByTitle instead
    @Deprecated
    @Query("SELECT c FROM Conversation c WHERE c.conversationType = 'GROUP' AND c.title LIKE %:query%")
    Page<Conversation> findGroupsByTitle(@Param("query") String query, Pageable pageable);

    // ✅ ACTIVE-AWARE: Group search with active filter
    @Query("SELECT c FROM Conversation c WHERE c.conversationType = 'GROUP' AND c.title LIKE %:query% AND c.isActive = true")
    Page<Conversation> findActiveGroupsByTitle(@Param("query") String query, Pageable pageable);

    // ========== STATISTICS & ANALYTICS ==========
    
    // ✅ GOOD: Explicit active condition
    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.universityContext.universityId = :universityId AND c.isActive = true")
    long countActiveConversationsByUniversity(@Param("universityId") Integer universityId);
    
    // ✅ GOOD: Explicit active conditions
    @Query("SELECT COUNT(c) FROM Conversation c JOIN c.participants cp WHERE cp.user.userId = :userId AND cp.isActive = true AND c.isActive = true AND c.isArchived = false")
    long countActiveConversationsByUserId(@Param("userId") Integer userId);
    
    // @deprecated - Use findActiveConversationsByMessageCount instead
    @Deprecated
    @Query("SELECT c FROM Conversation c WHERE c.messageCount > :minMessageCount ORDER BY c.messageCount DESC")
    Page<Conversation> findMostActiveConversations(@Param("minMessageCount") Integer minMessageCount, Pageable pageable);
    
    // ✅ OPTIMIZED: Use tuple projection for statistics
    @Query("SELECT c.conversationId, c.title, c.messageCount, c.participantCount, c.lastActivityAt " +
           "FROM Conversation c WHERE c.messageCount > :minMessageCount AND c.isActive = true ORDER BY c.messageCount DESC")
    Page<Object[]> findActiveConversationsByMessageCountProjection(@Param("minMessageCount") Integer minMessageCount, Pageable pageable);
    
    // @deprecated - Use findActiveLargeConversations instead
    @Deprecated
    @Query("SELECT c FROM Conversation c WHERE c.participantCount > :minParticipants ORDER BY c.participantCount DESC")
    List<Conversation> findLargestConversations(@Param("minParticipants") Integer minParticipants, Pageable pageable);

    // ✅ ACTIVE-AWARE: Largest conversations with active filter
    @Query("SELECT c FROM Conversation c WHERE c.participantCount > :minParticipants AND c.isActive = true ORDER BY c.participantCount DESC")
    List<Conversation> findActiveLargeConversations(@Param("minParticipants") Integer minParticipants, Pageable pageable);

    // ========== ONLINE STATUS QUERIES ==========
    
    // @deprecated - Use findOnlineActiveConversations instead
    @Deprecated
    @Query("SELECT c FROM Conversation c WHERE c.onlineParticipantCount > 0 ORDER BY c.lastActivityAt DESC")
    Page<Conversation> findActiveConversationsWithOnlineUsers(Pageable pageable);
    
    // ✅ ACTIVE-AWARE: Online conversations with active filter
    @Query("SELECT c FROM Conversation c WHERE c.onlineParticipantCount > 0 AND c.isActive = true ORDER BY c.lastActivityAt DESC")
    Page<Conversation> findOnlineActiveConversations(Pageable pageable);
    
    // @deprecated - Use findActiveLargeGroupsWithRecentActivity instead
    @Deprecated
    @Query("SELECT c FROM Conversation c WHERE c.recentActiveParticipantCount > :threshold AND c.isLargeGroup = true")
    List<Conversation> findLargeGroupsWithRecentActivity(@Param("threshold") Integer threshold);

    // ✅ OPTIMIZED: Large groups with recent activity - minimal data
    @Query("SELECT c.conversationId, c.recentActiveParticipantCount, c.participantCount " +
           "FROM Conversation c WHERE c.recentActiveParticipantCount > :threshold AND c.isLargeGroup = true AND c.isActive = true")
    List<Object[]> findActiveLargeGroupsWithRecentActivityData(@Param("threshold") Integer threshold);

    // ✅ OPTIMIZED: For online status job - only fetch necessary fields
    @Query("SELECT c.conversationId, c.onlineParticipantCount, c.recentActiveParticipantCount, c.lastActivityAt " +
           "FROM Conversation c WHERE c.lastActivityAt > :since AND c.isActive = true")
    List<Object[]> findConversationsWithRecentActivity(@Param("since") Instant since);

    // ========== EXISTENCE CHECKS ==========
    
    // @deprecated - Use existsActiveByConversationId instead
    @Deprecated
    boolean existsByConversationId(Integer conversationId);
    
    // ✅ GOOD: Explicit active condition
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Conversation c WHERE c.conversationId = :conversationId AND c.isActive = true")
    boolean existsActiveByConversationId(@Param("conversationId") Integer conversationId);

    // ========== ADDITIONAL ACTIVE-AWARE METHODS ==========

    // ✅ ACTIVE-AWARE: Count methods
    long countByConversationTypeAndIsActiveTrue(ConversationType conversationType);
    long countByUniversityContext_UniversityIdAndIsActiveTrue(Integer universityId);
    long countByIsArchivedAndIsActiveTrue(boolean isArchived);

    Optional<Conversation> findByIdAndIsActiveTrue(Integer id);
}