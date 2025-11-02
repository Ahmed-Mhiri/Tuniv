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
import com.tuniv.backend.chat.projection.participant.ParticipantProjection;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, ConversationParticipant.ConversationParticipantId>,
        JpaSpecificationExecutor<ConversationParticipant> {

    // ===================================================================================
    // ========== Finders (Full Entity)
    // ===================================================================================

    /**
     * Finds an ACTIVE participant by conversation and user ID.
     */
    @EntityGraph(attributePaths = {"user", "role", "conversation"})
    Optional<ConversationParticipant> findByConversation_ConversationIdAndUser_UserIdAndIsActiveTrue(Integer conversationId, Integer userId);

    /**
     * Finds an ACTIVE participant by conversation object and user ID.
     */
    @EntityGraph(attributePaths = {"user", "role"})
    Optional<ConversationParticipant> findByConversationAndUser_UserIdAndIsActiveTrue(Conversation conversation, Integer userId);
    
    /**
     * Finds ANY participant (active or inactive) by conversation and user IDs.
     * Useful for checking if a user *ever* joined.
     */
    @EntityGraph(attributePaths = {"user", "role", "conversation"})
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.user.userId = :userId")
    Optional<ConversationParticipant> findByConversationIdAndUserId(@Param("conversationId") Integer conversationId,
                                                                    @Param("userId") Integer userId);

    /**
     * Finds all ACTIVE participants in a specific conversation.
     */
    @EntityGraph(attributePaths = {"user", "role"})
    List<ConversationParticipant> findByConversationAndIsActiveTrue(Conversation conversation);

    /**
     * Finds all ACTIVE participants in a specific conversation by its ID.
     */
    @EntityGraph(attributePaths = {"user", "role"})
    List<ConversationParticipant> findByConversation_ConversationIdAndIsActiveTrue(Integer conversationId);

    /**
     * Finds all ACTIVE participants for a user (i.e., all their active conversations).
     */
    @EntityGraph(attributePaths = {"conversation", "role"})
    List<ConversationParticipant> findByUser_UserIdAndIsActiveTrue(Integer userId);

    /**
     * Finds a paginated list of ACTIVE participants for a user.
     */
    @EntityGraph(attributePaths = {"conversation", "role"})
    Page<ConversationParticipant> findByUser_UserIdAndIsActiveTrue(Integer userId, Pageable pageable);

    /**
     * Finds all ACTIVE participants for a user across a specific list of conversations.
     */
    @EntityGraph(attributePaths = {"user", "role"})
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId IN :conversationIds AND cp.user.userId = :userId AND cp.isActive = true")
    List<ConversationParticipant> findByConversationIdsAndUserIdAndIsActiveTrue(
            @Param("conversationIds") List<Integer> conversationIds,
            @Param("userId") Integer userId
    );

    /**
     * Finds all ACTIVE participants across a list of conversation IDs.
     */
    @EntityGraph(attributePaths = {"user", "role"})
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId IN :conversationIds AND cp.isActive = true")
    List<ConversationParticipant> findByConversation_ConversationIdInAndIsActiveTrue(
            @Param("conversationIds") List<Integer> conversationIds
    );

    /**
     * Finds all ACTIVE participants in a conversation with a specific role ID.
     */
    @EntityGraph(attributePaths = {"user", "role"})
    List<ConversationParticipant> findByConversationAndRole_IdAndIsActiveTrue(Conversation conversation, Integer roleId);

    /**
     * Finds all ACTIVE participants in a conversation with a specific role name.
     */
    @EntityGraph(attributePaths = {"user", "role"})
    List<ConversationParticipant> findByConversationAndRole_NameAndIsActiveTrue(Conversation conversation, String roleName);

    /**
     * Finds all ACTIVE participants in a conversation (by ID) with a specific role name.
     */
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.role.name = :roleName AND cp.isActive = true")
    List<ConversationParticipant> findByConversationIdAndRoleNameAndIsActiveTrue(@Param("conversationId") Integer conversationId,
                                                                                 @Param("roleName") String roleName);

    /**
     * Finds all ACTIVE admins (by role name) in a specific conversation.
     * This returns the full ConversationParticipant entity.
     */
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.role.name IN ('CONVERSATION_ADMIN', 'conversation_admin') AND cp.isActive = true")
    List<ConversationParticipant> findActiveAdminParticipantsByConversationId(@Param("conversationId") Integer conversationId);

    /**
     * Finds all ACTIVE muted participants across all conversations.
     */
    @EntityGraph(attributePaths = {"user", "conversation"})
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.isMuted = true AND cp.isActive = true")
    List<ConversationParticipant> findActiveMutedParticipants();

    /**
     * Finds all ACTIVE participants whose mute expires after a certain time.
     */
    @EntityGraph(attributePaths = {"user", "conversation"})
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.isMuted = true AND cp.mutedUntil > :time AND cp.isActive = true")
    List<ConversationParticipant> findActiveMutedParticipantsUntilAfter(@Param("time") Instant time);

    /**
     * Finds all ACTIVE muted participants in a specific conversation.
     */
    @EntityGraph(attributePaths = {"user"})
    List<ConversationParticipant> findByConversationAndIsMutedTrueAndIsActiveTrue(Conversation conversation);

    /**
     * Finds all ACTIVE participants (across all conversations) with notifications disabled.
     */
    @EntityGraph(attributePaths = {"user", "conversation"})
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.notificationsEnabled = false AND cp.isActive = true")
    List<ConversationParticipant> findActiveParticipantsWithDisabledNotifications();

    /**
     * Finds all ACTIVE participants in a specific conversation with notifications disabled.
     */
    @EntityGraph(attributePaths = {"user"})
    List<ConversationParticipant> findByConversationAndNotificationsEnabledFalseAndIsActiveTrue(Conversation conversation);

    /**
     * Finds participants who have been inactive since a specific time.
     */
    @EntityGraph(attributePaths = {"user", "conversation"})
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.lastActiveAt < :threshold AND cp.isActive = true")
    List<ConversationParticipant> findInactiveParticipantsSince(@Param("threshold") Instant threshold);

    /**
     * Finds recently active participants in a conversation, paginated.
     */
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.lastActiveAt IS NOT NULL AND cp.isActive = true ORDER BY cp.lastActiveAt DESC")
    Page<ConversationParticipant> findActiveRecentlyActiveParticipants(@Param("conversationId") Integer conversationId, Pageable pageable);

    /**
     * Finds participants who have read messages after a specific timestamp, paginated.
     */
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation = :conversation AND cp.lastReadTimestamp >= :timestamp AND cp.isActive = true ORDER BY cp.lastReadTimestamp DESC")
    List<ConversationParticipant> findByConversationAndLastReadTimestampAfter(
            @Param("conversation") Conversation conversation,
            @Param("timestamp") Instant timestamp,
            Pageable pageable
    );
    
    /**
     * Finds participants with unread messages (based on a 'since' timestamp).
     */
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.lastReadTimestamp < :since AND cp.isActive = true")
    List<ConversationParticipant> findActiveParticipantsWithUnreadMessages(@Param("conversationId") Integer conversationId,
                                                                           @Param("since") Instant since);
                                                                           
    /**
     * Finds participants with an unread count greater than zero.
     */
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.unreadCount > 0 AND cp.isActive = true")
    List<ConversationParticipant> findActiveParticipantsWithUnreadCount(@Param("conversationId") Integer conversationId);

    /**
     * Bulk fetch of active participants by user IDs.
     */
    @EntityGraph(attributePaths = {"user", "role"})
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.user.userId IN :userIds AND cp.isActive = true")
    List<ConversationParticipant> findByUserIdsAndIsActiveTrueWithUserAndRole(@Param("userIds") List<Integer> userIds);

    /**
     * ⚠️ KEPT FOR BACKWARD COMPATIBILITY: Fetches ALL participants (active or not).
     * Use with caution, prefer {@link #findByConversation_ConversationIdAndIsActiveTrue(Integer)}
     */
    @EntityGraph(attributePaths = {"user", "role"})
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId")
    List<ConversationParticipant> findByConversation_ConversationId(@Param("conversationId") Integer conversationId);

    // ===================================================================================
    // ========== Finders (DTO Projection)
    // ===================================================================================

    /**
     * Finds all ACTIVE participants as a projection.
     */
    @Query("""
            SELECT 
                u.userId as userId, u.username as username, u.profilePhotoUrl as profilePhotoUrl,
                cp.nickname as nickname, r.id as roleId, r.name as roleName, r.displayName as displayName,
                cp.joinedAt as joinedAt, cp.lastActiveAt as lastActiveAt, cp.messageCount as messageCount,
                cp.isActive as isActive, cp.notificationsEnabled as notificationsEnabled,
                cp.isMuted as isMuted, cp.mutedUntil as mutedUntil,
                cp.unreadCount as unreadCount, cp.lastReadTimestamp as lastReadTimestamp
            FROM ConversationParticipant cp
            JOIN cp.user u
            JOIN cp.role r
            WHERE cp.conversation.conversationId = :conversationId 
            AND cp.isActive = true
            """)
    List<ParticipantProjection> findActiveParticipantsProjectionByConversationId(@Param("conversationId") Integer conversationId);

    /**
     * Finds a paginated list of ACTIVE participants as a projection.
     */
    @Query("""
            SELECT 
                u.userId as userId, u.username as username, u.profilePhotoUrl as profilePhotoUrl,
                cp.nickname as nickname, r.id as roleId, r.name as roleName, r.displayName as displayName,
                cp.joinedAt as joinedAt, cp.lastActiveAt as lastActiveAt, cp.messageCount as messageCount,
                cp.isActive as isActive, cp.notificationsEnabled as notificationsEnabled,
                cp.isMuted as isMuted, cp.mutedUntil as mutedUntil,
                cp.unreadCount as unreadCount, cp.lastReadTimestamp as lastReadTimestamp
            FROM ConversationParticipant cp
            JOIN cp.user u
            JOIN cp.role r
            WHERE cp.conversation.conversationId = :conversationId 
            AND cp.isActive = true
            """)
    Page<ParticipantProjection> findActiveParticipantsProjectionByConversationId(@Param("conversationId") Integer conversationId, Pageable pageable);

    /**
     * Optimized projection for large groups (minimal data).
     */
    @Query("SELECT cp.user.userId as userId, cp.user.username as username, cp.role.name as roleName " +
           "FROM ConversationParticipant cp " +
           "WHERE cp.conversation.conversationId = :conversationId AND cp.isActive = true " +
           "ORDER BY cp.role.name, cp.joinedAt ASC")
    Page<ParticipantProjection> findParticipantsForLargeGroup(@Param("conversationId") Integer conversationId, Pageable pageable);

    /**
     * Recently active participants summary (optimized projection).
     */
    @Query("SELECT cp.user.userId as userId, cp.user.username as username, " +
           "cp.user.profilePhotoUrl as profilePhotoUrl, cp.nickname as nickname, " +
           "cp.role.id as roleId, cp.role.name as roleName, cp.joinedAt as joinedAt " +
           "FROM ConversationParticipant cp " +
           "WHERE cp.conversation.conversationId = :conversationId AND cp.isActive = true " +
           "ORDER BY cp.lastActiveAt DESC NULLS LAST")
    List<ParticipantProjection> findRecentlyActiveParticipantsSummary(@Param("conversationId") Integer conversationId, Pageable pageable);

    /**
     * Participants active since a specific time (optimized projection).
     */
    @Query("SELECT cp.user.userId as userId, cp.user.username as username, cp.lastActiveAt as lastActiveAt " +
           "FROM ConversationParticipant cp " +
           "WHERE cp.conversation.conversationId = :conversationId AND cp.isActive = true AND cp.lastActiveAt >= :since")
    List<ParticipantProjection> findParticipantsActiveSince(@Param("conversationId") Integer conversationId,
                                                              @Param("since") Instant since);

    /**
     * Finds admin participants (by role names) as a projection.
     */
    @Query("SELECT cp.user.userId as userId, cp.user.username as username " +
           "FROM ConversationParticipant cp " +
           "WHERE cp.conversation.conversationId = :conversationId AND cp.role.name IN :roleNames AND cp.isActive = true")
    List<ParticipantProjection> findActiveAdminProjectionsByConversationId(@Param("conversationId") Integer conversationId,
                                                                         @Param("roleNames") List<String> roleNames);


    // ===================================================================================
    // ========== Existence & Count Queries
    // ===================================================================================

    /**
     * Checks if a user is an active participant in a conversation.
     */
    boolean existsByConversationAndUser_UserIdAndIsActive(Conversation conversation, Integer userId, boolean active);
    
    /**
     * Checks if a user is an active participant by conversation and user IDs.
     */
    @Query("SELECT COUNT(cp) > 0 FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.user.userId = :userId AND cp.isActive = true")
    boolean isUserActiveParticipant(@Param("conversationId") Integer conversationId, @Param("userId") Integer userId);

    /**
     * Counts all active participants in a conversation.
     */
    long countByConversationAndIsActiveTrue(Conversation conversation);

    /**
     * Counts participants in a conversation by their active status.
     */
    long countByConversationAndIsActive(Conversation conversation, boolean active);

    /**
     * Counts all active participants in a conversation by its ID.
     */
    @Query("SELECT COUNT(cp) FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.isActive = true")
    long countActiveParticipantsByConversationId(@Param("conversationId") Integer conversationId);

    /**
     * Counts all active conversations for a user.
     */
    long countByUser_UserIdAndIsActiveTrue(@Param("userId") Integer userId);

    /**
     * Counts participants who have read messages after a specific timestamp.
     */
    @Query("SELECT COUNT(cp) FROM ConversationParticipant cp WHERE cp.conversation = :conversation AND cp.lastReadTimestamp >= :timestamp AND cp.isActive = true")
    long countByConversationAndLastReadTimestampAfter(
            @Param("conversation") Conversation conversation,
            @Param("timestamp") Instant timestamp
    );
    
    /**
     * Fetches just the user IDs of active participants in a conversation.
     * Optimized for sending notifications or presence updates.
     */
    @Query("SELECT cp.user.userId FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.isActive = true")
    List<Integer> findActiveUserIdsByConversationId(@Param("conversationId") Integer conversationId);


    // ===================================================================================
    // ========== Update & Modify Operations
    // ===================================================================================

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

    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.isActive = false WHERE cp.conversation.conversationId = :conversationId")
    void deactivateAllParticipants(@Param("conversationId") Integer conversationId);

    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.role.id = :roleId WHERE cp.conversation.conversationId = :conversationId AND cp.user.userId IN :userIds")
    void updateParticipantsRole(@Param("conversationId") Integer conversationId,
                                @Param("userIds") List<Integer> userIds,
                                @Param("roleId") Integer roleId);
                                
    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.lastReadTimestamp = :timestamp WHERE cp.conversation.conversationId = :conversationId AND cp.user.userId IN :userIds")
    int batchUpdateLastReadTimestamp(@Param("conversationId") Integer conversationId,
                                     @Param("userIds") List<Integer> userIds,
                                     @Param("timestamp") Instant timestamp);

    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.unreadCount = cp.unreadCount + :increment WHERE cp.conversation.conversationId = :conversationId AND cp.user.userId IN :userIds")
    int batchIncrementUnreadCount(@Param("conversationId") Integer conversationId,
                                  @Param("userIds") List<Integer> userIds,
                                  @Param("increment") Integer increment);
    
    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.lastActiveAt = :timestamp WHERE cp.conversation.conversationId = :conversationId AND cp.user.userId IN :userIds")
    int batchUpdateLastActiveAt(@Param("conversationId") Integer conversationId,
                                @Param("userIds") List<Integer> userIds,
                                @Param("timestamp") Instant timestamp);


    // ===================================================================================
    // ========== Deprecated Methods (for backward compatibility)
    // ===================================================================================

    /**
     * @deprecated Use {@link #findByConversationAndIsActiveTrue(Conversation)} instead
     */
    @Deprecated
    @EntityGraph(attributePaths = {"user", "role"})
    List<ConversationParticipant> findByConversation(Conversation conversation);

    /**
     * @deprecated Use {@link #findByUser_UserIdAndIsActiveTrue(Integer)} instead
     */
    @Deprecated
    @EntityGraph(attributePaths = {"conversation", "role"})
    List<ConversationParticipant> findByUser_UserId(Integer userId);

    /**
     * @deprecated Use {@link #existsByConversationAndUser_UserIdAndIsActive(Conversation, Integer, boolean)} instead
     */
    @Deprecated
    boolean existsByConversationAndUser_UserId(Conversation conversation, Integer userId);

    /**
     * @deprecated Use {@link #countByConversationAndIsActiveTrue(Conversation)} instead
     */
    @Deprecated
    long countByConversation(Conversation conversation);

    /**
     * @deprecated Use {@link #findByConversationAndRole_IdAndIsActiveTrue(Conversation, Integer)} instead
     */
    @Deprecated
    @EntityGraph(attributePaths = {"user", "role"})
    List<ConversationParticipant> findByConversationAndRole_Id(Conversation conversation, Integer roleId);

    /**
     * @deprecated Use {@link #findByConversationAndRole_NameAndIsActiveTrue(Conversation, String)} instead
     */
    @Deprecated
    @EntityGraph(attributePaths = {"user", "role"})
    List<ConversationParticipant> findByConversationAndRole_Name(Conversation conversation, String roleName);

    /**
     * @deprecated Use {@link #findActiveMutedParticipants()} instead
     */
    @Deprecated
    @EntityGraph(attributePaths = {"user", "conversation"})
    List<ConversationParticipant> findByIsMutedTrue();

    /**
     * @deprecated Use {@link #findActiveMutedParticipantsUntilAfter(Instant)} instead
     */
    @Deprecated
    @EntityGraph(attributePaths = {"user", "conversation"})
    List<ConversationParticipant> findByIsMutedTrueAndMutedUntilAfter(Instant time);

    /**
     * @deprecated Use {@link #findByConversationAndIsMutedTrueAndIsActiveTrue(Conversation)} instead
     */
    @Deprecated
    @EntityGraph(attributePaths = {"user"})
    List<ConversationParticipant> findByConversationAndIsMutedTrue(Conversation conversation);

    /**
     * @deprecated Use {@link #findActiveParticipantsWithDisabledNotifications()} instead
     */
    @Deprecated
    @EntityGraph(attributePaths = {"user", "conversation"})
    List<ConversationParticipant> findByNotificationsEnabledFalse();

    /**
     * @deprecated Use {@link #findByConversationAndNotificationsEnabledFalseAndIsActiveTrue(Conversation)} instead
     */
    @Deprecated
    @EntityGraph(attributePaths = {"user"})
    List<ConversationParticipant> findByConversationAndNotificationsEnabledFalse(Conversation conversation);

    /**
     * @deprecated Use {@link #findActiveRecentlyActiveParticipants(Integer, Pageable)} instead
     */
    @Deprecated
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.lastActiveAt IS NOT NULL ORDER BY cp.lastActiveAt DESC")
    Page<ConversationParticipant> findRecentlyActiveParticipants(@Param("conversationId") Integer conversationId, Pageable pageable);

    /**
     * @deprecated Use {@link #findByConversation_ConversationIdInAndIsActiveTrue(List)} instead
     */
    @Deprecated
    @EntityGraph(attributePaths = {"user", "role"})
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId IN :conversationIds")
    List<ConversationParticipant> findByConversationIds(@Param("conversationIds") List<Integer> conversationIds);
}