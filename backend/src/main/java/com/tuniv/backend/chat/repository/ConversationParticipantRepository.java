package com.tuniv.backend.chat.repository;

import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, ConversationParticipant.ConversationParticipantId>, 
                                                           JpaSpecificationExecutor<ConversationParticipant> {

    // ========== Basic Finders ==========
    
    Optional<ConversationParticipant> findByConversationAndUser_UserId(Conversation conversation, Integer userId);
    
    // ADDED: The required method for PermissionService
    Optional<ConversationParticipant> findByConversation_ConversationIdAndUser_UserId(Integer conversationId, Integer userId);
    
    List<ConversationParticipant> findByConversation(Conversation conversation);
    
    List<ConversationParticipant> findByConversationAndIsActiveTrue(Conversation conversation);
    
    List<ConversationParticipant> findByUser_UserId(Integer userId);
    
    List<ConversationParticipant> findByUser_UserIdAndIsActiveTrue(Integer userId);
    
    Page<ConversationParticipant> findByUser_UserIdAndIsActiveTrue(Integer userId, Pageable pageable);

    // ========== Existence Checks ==========
    
    boolean existsByConversationAndUser_UserId(Conversation conversation, Integer userId);
    
    boolean existsByConversationAndUser_UserIdAndIsActive(Conversation conversation, Integer userId, boolean active);
    
    @Query("SELECT COUNT(cp) > 0 FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.user.userId = :userId AND cp.isActive = true")
    boolean isUserActiveParticipant(@Param("conversationId") Integer conversationId, @Param("userId") Integer userId);

    // ========== Count Queries ==========
    
    long countByConversation(Conversation conversation);
    
    long countByConversationAndIsActive(Conversation conversation, boolean active);
    
    @Query("SELECT COUNT(cp) FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.isActive = true")
    long countActiveParticipantsByConversationId(@Param("conversationId") Integer conversationId);
    
    @Query("SELECT COUNT(cp) FROM ConversationParticipant cp WHERE cp.user.userId = :userId AND cp.isActive = true")
    long countActiveConversationsByUser(@Param("userId") Integer userId);

    // ========== Role-Based Queries ==========
    
    List<ConversationParticipant> findByConversationAndRole_Id(Conversation conversation, Integer roleId);
    
    List<ConversationParticipant> findByConversationAndRole_Name(Conversation conversation, String roleName);
    
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.role.name = :roleName AND cp.isActive = true")
    List<ConversationParticipant> findActiveParticipantsByRole(@Param("conversationId") Integer conversationId, 
                                                              @Param("roleName") String roleName);

    // ========== Status-Based Queries ==========
    
    List<ConversationParticipant> findByIsMutedTrue();
    
    List<ConversationParticipant> findByIsMutedTrueAndMutedUntilAfter(Instant time);
    
    List<ConversationParticipant> findByConversationAndIsMutedTrue(Conversation conversation);
    
    List<ConversationParticipant> findByNotificationsEnabledFalse();
    
    List<ConversationParticipant> findByConversationAndNotificationsEnabledFalse(Conversation conversation);

    // ========== Activity Queries ==========
    
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.lastActiveAt < :threshold AND cp.isActive = true")
    List<ConversationParticipant> findInactiveParticipantsSince(@Param("threshold") Instant threshold);
    
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.lastActiveAt IS NOT NULL ORDER BY cp.lastActiveAt DESC")
    Page<ConversationParticipant> findRecentlyActiveParticipants(@Param("conversationId") Integer conversationId, Pageable pageable);

    // ========== Update Queries ==========
    
    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.isActive = false WHERE cp.conversation.conversationId = :conversationId AND cp.user.userId = :userId")
    void deactivateParticipant(@Param("conversationId") Integer conversationId, @Param("userId") Integer userId);
    
    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.lastReadTimestamp = :timestamp WHERE cp.conversation.conversationId = :conversationId AND cp.user.userId = :userId")
    void updateLastReadTimestamp(@Param("conversationId") Integer conversationId, 
                                @Param("userId") Integer userId, 
                                @Param("timestamp") Instant timestamp);
    
    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.unreadCount = :unreadCount WHERE cp.conversation.conversationId = :conversationId AND cp.user.userId = :userId")
    void updateUnreadCount(@Param("conversationId") Integer conversationId, 
                          @Param("userId") Integer userId, 
                          @Param("unreadCount") Integer unreadCount);
    
    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.isMuted = :isMuted, cp.mutedUntil = :mutedUntil WHERE cp.conversation.conversationId = :conversationId AND cp.user.userId = :userId")
    void updateMuteStatus(@Param("conversationId") Integer conversationId, 
                         @Param("userId") Integer userId, 
                         @Param("isMuted") boolean isMuted, 
                         @Param("mutedUntil") Instant mutedUntil);

    // ========== Bulk Operations ==========
    
    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.isActive = false WHERE cp.conversation.conversationId = :conversationId")
    void deactivateAllParticipants(@Param("conversationId") Integer conversationId);
    
    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.role.id = :roleId WHERE cp.conversation.conversationId = :conversationId AND cp.user.userId IN :userIds")
    void updateParticipantsRole(@Param("conversationId") Integer conversationId, 
                               @Param("userIds") List<Integer> userIds, 
                               @Param("roleId") Integer roleId);
    
    // ========== Additional Query Methods ==========
    
    List<ConversationParticipant> findByConversation_ConversationIdAndIsActiveTrue(Integer conversationId);

    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId IN :conversationIds AND cp.user.userId = :userId AND cp.isActive = true")
    List<ConversationParticipant> findByConversation_ConversationIdInAndUser_UserIdAndIsActiveTrue(
        @Param("conversationIds") List<Integer> conversationIds, 
        @Param("userId") Integer userId
    );

    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId IN :conversationIds AND cp.user.userId = :userId AND cp.isActive = true")
    List<ConversationParticipant> findByConversationIdsAndUserId(
        @Param("conversationIds") List<Integer> conversationIds,
        @Param("userId") Integer userId
    );

    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId IN :conversationIds AND cp.isActive = true")
    List<ConversationParticipant> findByConversation_ConversationIdInAndIsActiveTrue(
        @Param("conversationIds") List<Integer> conversationIds
    );

    @Query("SELECT COUNT(cp) FROM ConversationParticipant cp WHERE cp.conversation = :conversation AND cp.lastReadTimestamp >= :timestamp AND cp.isActive = true")
    long countByConversationAndLastReadTimestampAfter(
        @Param("conversation") Conversation conversation,
        @Param("timestamp") Instant timestamp
    );

    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation = :conversation AND cp.lastReadTimestamp >= :timestamp AND cp.isActive = true ORDER BY cp.lastReadTimestamp DESC")
    List<ConversationParticipant> findByConversationAndLastReadTimestampAfter(
        @Param("conversation") Conversation conversation,
        @Param("timestamp") Instant timestamp,
        Pageable pageable
    );

@Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId IN :conversationIds AND cp.isActive = true")
List<ConversationParticipant> findByConversationIds(@Param("conversationIds") List<Integer> conversationIds);
}