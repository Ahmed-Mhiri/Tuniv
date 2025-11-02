package com.tuniv.backend.chat.repository;

import java.util.List;
import java.util.Map;
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

import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.model.Reaction;
import com.tuniv.backend.chat.projection.reaction.ReactionProjection;
import com.tuniv.backend.chat.projection.reaction.ReactionSummaryProjection;
import com.tuniv.backend.user.model.User;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Integer>, JpaSpecificationExecutor<Reaction> {

    // ========== BASIC FINDERS WITH ENTITY GRAPHS ==========
    
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT r FROM Reaction r WHERE r.message = :message AND r.isRemoved = false")
    List<Reaction> findByMessageAndIsRemovedFalse(@Param("message") Message message);
    
    @EntityGraph(attributePaths = {"message", "message.conversation"})
    @Query("SELECT r FROM Reaction r WHERE r.user = :user AND r.isRemoved = false")
    List<Reaction> findByUserAndIsRemovedFalse(@Param("user") User user);
    
    @EntityGraph(attributePaths = {"user"})
    Optional<Reaction> findByMessageAndUserAndEmojiAndIsRemovedFalse(Message message, User user, String emoji);

    // ✅ NEW METHOD ADDED: Find reaction regardless of removal status
    @EntityGraph(attributePaths = {"user"})
    Optional<Reaction> findByMessageAndUserAndEmoji(Message message, User user, String emoji);

    // ✅ ADDED MISSING METHOD: Find by ID with removal check
    @Query("SELECT r FROM Reaction r WHERE r.id = :id AND r.isRemoved = false")
    Optional<Reaction> findByIdAndIsRemovedFalse(@Param("id") Integer id);

    // ========== DTO PROJECTIONS FOR HIGH-TRAFFIC ENDPOINTS ==========
    
    @Query("SELECT r.id as id, r.message.id as messageId, r.user.id as userId, " +
           "r.user.username as username, r.user.profilePhotoUrl as profilePhotoUrl, " +
           "r.emoji as emoji, r.skinTone as skinTone, r.customText as customText, " +
           "r.createdAt as createdAt, r.isRemoved as isRemoved, r.removedAt as removedAt " +
           "FROM Reaction r WHERE r.message.id = :messageId AND r.isRemoved = false")
    List<ReactionProjection> findActiveReactionsByMessageId(@Param("messageId") Integer messageId);
    
    @Query("SELECT r.message.id as messageId, r.emoji as emoji, COUNT(r) as count " +
           "FROM Reaction r WHERE r.message.id IN :messageIds AND r.isRemoved = false " +
           "GROUP BY r.message.id, r.emoji")
    List<ReactionSummaryProjection> getReactionCountsByMessageIds(@Param("messageIds") List<Integer> messageIds);
    
    @Query("SELECT r.emoji as emoji, COUNT(r) as count " +
           "FROM Reaction r WHERE r.message.id = :messageId AND r.isRemoved = false " +
           "GROUP BY r.emoji ORDER BY count DESC")
    List<ReactionSummaryProjection> getReactionSummaryByMessageId(@Param("messageId") Integer messageId);

    // ========== BULK OPERATIONS WITH ENTITY GRAPHS ==========
    
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT r FROM Reaction r WHERE r.message.id IN :messageIds AND r.isRemoved = false")
    List<Reaction> findByMessageIdsInAndIsRemovedFalseWithUser(@Param("messageIds") List<Integer> messageIds);
    
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT r FROM Reaction r WHERE r.message IN :messages AND r.isRemoved = false")
    List<Reaction> findByMessagesInAndIsRemovedFalseWithUser(@Param("messages") List<Message> messages);

    // ========== EMOJI-BASED QUERIES WITH OPTIMIZED FETCHING ==========
    
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT r FROM Reaction r WHERE r.message = :message AND r.emoji = :emoji AND r.isRemoved = false")
    List<Reaction> findByMessageAndEmojiAndIsRemovedFalseWithUser(@Param("message") Message message, @Param("emoji") String emoji);
    
    @Query(value = "SELECT r.emoji, COUNT(r) as count FROM Reaction r " +
           "WHERE r.message.id = :messageId AND r.isRemoved = false " +
           "GROUP BY r.emoji ORDER BY count DESC LIMIT 5", 
           nativeQuery = true)
    List<Object[]> findTopEmojisByMessageId(@Param("messageId") Integer messageId);

    // ========== STATISTICS AND AGGREGATIONS ==========
    
    @Query("SELECT COUNT(r) FROM Reaction r WHERE r.message.id = :messageId AND r.isRemoved = false")
    long countActiveReactionsByMessageId(@Param("messageId") Integer messageId);
    
    @Query("SELECT COUNT(r) FROM Reaction r WHERE r.user.id = :userId AND r.isRemoved = false")
    long countByUserIdAndIsRemovedFalse(@Param("userId") Integer userId);
    
    @Query("SELECT COUNT(r) FROM Reaction r WHERE r.message.conversation.id = :conversationId AND r.isRemoved = false")
    long countActiveReactionsByConversationId(@Param("conversationId") Integer conversationId);

    // ========== EXISTENCE CHECKS ==========
    
    boolean existsByMessageAndUserAndEmojiAndIsRemovedFalse(Message message, User user, String emoji);
    
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reaction r " +
           "WHERE r.message.id = :messageId AND r.user.id = :userId AND r.isRemoved = false")
    boolean existsActiveByMessageIdAndUserId(@Param("messageId") Integer messageId, @Param("userId") Integer userId);

    // ========== BATCH UPDATE OPERATIONS ==========
    
    @Modifying
    @Query("UPDATE Reaction r SET r.isRemoved = true, r.removedAt = CURRENT_TIMESTAMP " +
           "WHERE r.message.id = :messageId AND r.user.id = :userId AND r.emoji = :emoji")
    void removeSpecificReaction(@Param("messageId") Integer messageId, 
                              @Param("userId") Integer userId, 
                              @Param("emoji") String emoji);
    
    @Modifying
    @Query("UPDATE Reaction r SET r.isRemoved = true, r.removedAt = CURRENT_TIMESTAMP " +
           "WHERE r.message.id IN :messageIds")
    void removeAllReactionsFromMessages(@Param("messageIds") List<Integer> messageIds);
    
    @Modifying
    @Query("UPDATE Reaction r SET r.isRemoved = true, r.removedAt = CURRENT_TIMESTAMP " +
           "WHERE r.user.id = :userId AND r.message.conversation.id = :conversationId")
    void removeUserReactionsFromConversation(@Param("userId") Integer userId, 
                                           @Param("conversationId") Integer conversationId);

    // ========== ADVANCED ANALYTICS QUERIES ==========
    
    @Query("SELECT r.user.id as userId, r.user.username as username, COUNT(r) as reactionCount " +
           "FROM Reaction r WHERE r.message.conversation.id = :conversationId AND r.isRemoved = false " +
           "GROUP BY r.user.id, r.user.username ORDER BY reactionCount DESC")
    Page<ReactionProjection> findTopReactorsByConversation(@Param("conversationId") Integer conversationId, Pageable pageable);
    
    @Query("SELECT r.emoji as emoji, COUNT(r) as count, r.message.conversation.id as conversationId " +
           "FROM Reaction r WHERE r.message.conversation.id IN :conversationIds AND r.isRemoved = false " +
           "GROUP BY r.message.conversation.id, r.emoji")
    List<ReactionSummaryProjection> getReactionTrendsByConversations(@Param("conversationIds") List<Integer> conversationIds);
    
    @Query("SELECT FUNCTION('DATE', r.createdAt) as reactionDate, COUNT(r) as dailyCount " +
           "FROM Reaction r WHERE r.message.conversation.id = :conversationId AND r.isRemoved = false " +
           "AND r.createdAt >= :since GROUP BY FUNCTION('DATE', r.createdAt) ORDER BY reactionDate DESC")
    List<Object[]> getDailyReactionStats(@Param("conversationId") Integer conversationId, 
                                       @Param("since") java.time.Instant since);

    // ========== PAGINATION SUPPORT ==========
    
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT r FROM Reaction r WHERE r.message.id = :messageId AND r.isRemoved = false")
    Page<Reaction> findActiveReactionsByMessageIdWithPagination(@Param("messageId") Integer messageId, Pageable pageable);
    
    @Query("SELECT r FROM Reaction r WHERE r.user.id = :userId AND r.isRemoved = false " +
           "ORDER BY r.createdAt DESC")
    Page<Reaction> findUserReactionsWithPagination(@Param("userId") Integer userId, Pageable pageable);

    // ========== USER-SPECIFIC REACTION DATA ==========
    
    @Query("SELECT r.message.id as messageId, r.emoji as emoji " +
           "FROM Reaction r WHERE r.message.id IN :messageIds AND r.user.id = :userId AND r.isRemoved = false")
    List<ReactionProjection> findUserReactionsForMessages(@Param("userId") Integer userId, 
                                                        @Param("messageIds") List<Integer> messageIds);
    
    @Query("SELECT r.emoji as emoji, COUNT(r) as count " +
           "FROM Reaction r WHERE r.user.id = :userId AND r.isRemoved = false " +
           "GROUP BY r.emoji ORDER BY count DESC")
    List<ReactionSummaryProjection> getUserReactionStats(@Param("userId") Integer userId);

    // ========== PERFORMANCE-OPTIMIZED BULK METHODS ==========
    
    @Query("SELECT NEW map(r.message.id, COUNT(r)) FROM Reaction r " +
           "WHERE r.message.id IN :messageIds AND r.isRemoved = false " +
           "GROUP BY r.message.id")
    Map<Integer, Long> getTotalReactionCountsByMessageIds(@Param("messageIds") List<Integer> messageIds);
    
    @Query("SELECT NEW map(r.message.id, r.emoji) FROM Reaction r " +
           "WHERE r.message.id IN :messageIds AND r.user.id = :userId AND r.isRemoved = false")
    Map<Integer, String> getUserReactionMapForMessages(@Param("userId") Integer userId, 
                                                     @Param("messageIds") List<Integer> messageIds);
}