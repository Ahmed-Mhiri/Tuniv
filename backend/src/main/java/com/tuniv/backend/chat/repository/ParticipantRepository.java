package com.tuniv.backend.chat.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.model.ConversationParticipant.ConversationParticipantId;

@Repository
public interface ParticipantRepository extends JpaRepository<ConversationParticipant, ConversationParticipantId>, 
                                               JpaSpecificationExecutor<ConversationParticipant> {

    // ========== BASIC FINDERS BY COMPOSITE ID COMPONENTS ==========
    Optional<ConversationParticipant> findById_UserIdAndId_ConversationId(Integer userId, Integer conversationId);
    List<ConversationParticipant> findById_ConversationId(Integer conversationId);
    List<ConversationParticipant> findById_UserId(Integer userId);
    
    // ========== CONVERSATION-CENTRIC QUERIES ==========
    List<ConversationParticipant> findByConversation(Conversation conversation);
    List<ConversationParticipant> findByConversationAndIsActiveTrue(Conversation conversation);
    Page<ConversationParticipant> findByConversation(Conversation conversation, Pageable pageable);
    Page<ConversationParticipant> findByConversationAndIsActiveTrue(Conversation conversation, Pageable pageable);
    
    // ========== USER-CENTRIC QUERIES ==========
    List<ConversationParticipant> findByUser_UserId(Integer userId);
    List<ConversationParticipant> findByUser_UserIdAndIsActiveTrue(Integer userId);
    Page<ConversationParticipant> findByUser_UserIdAndIsActiveTrue(Integer userId, Pageable pageable);

    // ========== BULK OPERATIONS ==========
    List<ConversationParticipant> findByConversationIn(List<Conversation> conversations);
    List<ConversationParticipant> findById_ConversationIdIn(List<Integer> conversationIds);
    List<ConversationParticipant> findById_UserIdIn(List<Integer> userIds);
    List<ConversationParticipant> findById_ConversationIdInAndIsActiveTrue(List<Integer> conversationIds);

    // ========== ROLE-BASED QUERIES ==========
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.role.name = :roleName AND cp.isActive = true")
    List<ConversationParticipant> findActiveParticipantsByRole(@Param("conversationId") Integer conversationId, 
                                                              @Param("roleName") String roleName);
    
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.role.name IN :roleNames AND cp.isActive = true")
    List<ConversationParticipant> findActiveParticipantsByRoles(@Param("conversationId") Integer conversationId, 
                                                               @Param("roleNames") List<String> roleNames);

    // ========== ACTIVITY & ONLINE STATUS ==========
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.isActive = true ORDER BY cp.lastActiveAt DESC NULLS LAST")
    Page<ConversationParticipant> findRecentlyActiveParticipants(@Param("conversationId") Integer conversationId, 
                                                               Pageable pageable);
    
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.isActive = true AND cp.lastActiveAt >= :since")
    List<ConversationParticipant> findParticipantsActiveSince(@Param("conversationId") Integer conversationId, 
                                                             @Param("since") Instant since);

    // ========== READ STATUS & NOTIFICATIONS ==========
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.lastReadTimestamp < :since")
    List<ConversationParticipant> findParticipantsWithUnreadMessages(@Param("conversationId") Integer conversationId, 
                                                                    @Param("since") Instant since);
    
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.unreadCount > 0")
    List<ConversationParticipant> findParticipantsWithUnreadCount(@Param("conversationId") Integer conversationId);
    
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.notificationsEnabled = true AND cp.isActive = true")
    List<ConversationParticipant> findParticipantsWithEnabledNotifications();

    // ========== MUTED PARTICIPANTS ==========
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.isMuted = true AND (cp.mutedUntil IS NULL OR cp.mutedUntil > :now)")
    List<ConversationParticipant> findCurrentlyMutedParticipants(@Param("conversationId") Integer conversationId, 
                                                               @Param("now") Instant now);

    // ========== STATISTICS ==========
    @Query("SELECT COUNT(cp) FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.isActive = true")
    long countActiveParticipantsByConversation(@Param("conversationId") Integer conversationId);
    
    @Query("SELECT COUNT(cp) FROM ConversationParticipant cp WHERE cp.user.userId = :userId AND cp.isActive = true")
    long countActiveConversationsByUser(@Param("userId") Integer userId);
    
    @Query("SELECT COUNT(cp) FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.role.name = 'CONVERSATION_ADMIN' AND cp.isActive = true")
    long countActiveAdminsByConversation(@Param("conversationId") Integer conversationId);

    // ========== UPDATE OPERATIONS ==========
    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.lastReadTimestamp = :timestamp WHERE cp.id.userId = :userId AND cp.id.conversationId = :conversationId")
    int updateLastReadTimestamp(@Param("conversationId") Integer conversationId, 
                               @Param("userId") Integer userId, 
                               @Param("timestamp") Instant timestamp);
    
    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.unreadCount = cp.unreadCount + :increment WHERE cp.id.userId = :userId AND cp.id.conversationId = :conversationId")
    int incrementUnreadCount(@Param("conversationId") Integer conversationId, 
                            @Param("userId") Integer userId, 
                            @Param("increment") Integer increment);
    
    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.unreadCount = 0 WHERE cp.id.userId = :userId AND cp.id.conversationId = :conversationId")
    int resetUnreadCount(@Param("conversationId") Integer conversationId, @Param("userId") Integer userId);

    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.lastActiveAt = :timestamp WHERE cp.id.userId = :userId AND cp.id.conversationId = :conversationId")
    int updateLastActiveAt(@Param("conversationId") Integer conversationId, 
                          @Param("userId") Integer userId, 
                          @Param("timestamp") Instant timestamp);

    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.isActive = false WHERE cp.id.userId = :userId AND cp.id.conversationId = :conversationId")
    int deactivateParticipant(@Param("conversationId") Integer conversationId, @Param("userId") Integer userId);

    // ========== ENTITY GRAPH METHODS ==========
    @EntityGraph(attributePaths = {"user", "role"})
    Optional<ConversationParticipant> findWithUserAndRoleById(ConversationParticipantId id);
    
    @EntityGraph(attributePaths = {"user"})
    List<ConversationParticipant> findById_ConversationIdWithUser(Integer conversationId);
    
    @EntityGraph(attributePaths = {"user", "role", "conversation"})
    Optional<ConversationParticipant> findWithUserAndRoleAndConversationById(ConversationParticipantId id);

    @EntityGraph(attributePaths = {"user", "role"})
    List<ConversationParticipant> findById_ConversationIdAndIsActiveTrueWithUserAndRole(Integer conversationId);

    // ========== EXISTENCE CHECKS ==========
    boolean existsById_UserIdAndId_ConversationId(Integer userId, Integer conversationId);
    boolean existsById_UserIdAndId_ConversationIdAndIsActiveTrue(Integer userId, Integer conversationId);
    boolean existsById_UserIdAndId_ConversationIdAndRole_Name(Integer userId, Integer conversationId, String roleName);
    
    @Query("SELECT CASE WHEN COUNT(cp) > 0 THEN true ELSE false END FROM ConversationParticipant cp WHERE cp.id.conversationId = :conversationId AND cp.id.userId = :userId AND cp.isActive = true")
    boolean isUserActiveParticipant(@Param("conversationId") Integer conversationId, @Param("userId") Integer userId);

    // ========== NICKNAME QUERIES ==========
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.nickname IS NOT NULL")
    List<ConversationParticipant> findParticipantsWithNicknames(@Param("conversationId") Integer conversationId);
    
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND LOWER(cp.nickname) = LOWER(:nickname)")
    Optional<ConversationParticipant> findByConversationAndNicknameIgnoreCase(@Param("conversationId") Integer conversationId, 
                                                                             @Param("nickname") String nickname);

    // ========== MESSAGE COUNT QUERIES ==========
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId ORDER BY cp.messageCount DESC")
    Page<ConversationParticipant> findTopContributors(@Param("conversationId") Integer conversationId, Pageable pageable);
    
    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.messageCount = cp.messageCount + 1 WHERE cp.id.userId = :userId AND cp.id.conversationId = :conversationId")
    int incrementMessageCount(@Param("conversationId") Integer conversationId, @Param("userId") Integer userId);
}