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
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.model.MessageType;
import com.tuniv.backend.chat.projection.message.MessageLightProjection;
import com.tuniv.backend.chat.projection.message.MessageListProjection;
import com.tuniv.backend.chat.projection.message.PinnedMessageProjection;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer>, JpaSpecificationExecutor<Message> {

    // ========== Basic Finders ==========
    
    // ⚠️ WARNING: Fetches ALL messages in conversation (including deleted)
    List<Message> findByConversation(Conversation conversation);
    
    // ⚠️ WARNING: Fetches ALL messages in conversation (including deleted)
    Page<Message> findByConversation(Conversation conversation, Pageable pageable);
    
    // ✅ GOOD: Explicit deleted condition
    List<Message> findByConversationAndDeletedFalse(Conversation conversation);
    
    // ✅ GOOD: Explicit deleted condition
    Page<Message> findByConversationAndDeletedFalse(Conversation conversation, Pageable pageable);
    
    // ⚠️ WARNING: Fetches ALL messages by user (including deleted)
    List<Message> findByAuthor_UserId(Integer userId);
    
    // ⚠️ WARNING: Fetches ALL messages by user (including deleted)
    Page<Message> findByAuthor_UserId(Integer userId, Pageable pageable);
    
    // ✅ GOOD: Explicit deleted condition
    List<Message> findByAuthor_UserIdAndDeletedFalse(Integer userId);
    
    // ✅ GOOD: Explicit deleted condition
    Page<Message> findByAuthor_UserIdAndDeletedFalse(Integer userId, Pageable pageable);
    
    // ⚠️ WARNING: Fetches ALL messages by type (including deleted)
    List<Message> findByMessageType(MessageType messageType);
    
    // ✅ GOOD: Explicit deleted condition
    List<Message> findByMessageTypeAndDeletedFalse(MessageType messageType);
    
    // ⚠️ WARNING: Fetches ALL messages by conversation and type (including deleted)
    List<Message> findByConversationAndMessageType(Conversation conversation, MessageType messageType);
    
    // ✅ GOOD: Explicit deleted condition
    List<Message> findByConversationAndMessageTypeAndDeletedFalse(Conversation conversation, MessageType messageType);

    // ========== Bulk Finders ==========
    
    // ⚠️ WARNING: Fetches ALL messages by IDs (including deleted)
    List<Message> findByIdIn(List<Integer> messageIds);
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT m FROM Message m WHERE m.id IN :messageIds AND m.deleted = false")
    List<Message> findByIdInAndDeletedFalse(@Param("messageIds") List<Integer> messageIds);
    
    // ⚠️ WARNING: Fetches ALL messages by conversation IDs (including deleted)
    List<Message> findByConversation_ConversationIdIn(List<Integer> conversationIds);
    
    // ✅ GOOD: Explicit deleted condition
    List<Message> findByConversation_ConversationIdInAndDeletedFalse(List<Integer> conversationIds);

    // ========== Complex Queries ==========
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.deleted = false ORDER BY m.sentAt DESC")
    List<Message> findLatestMessagesByConversation(@Param("conversationId") Integer conversationId, Pageable pageable);
    
    // ⚠️ WARNING: Finds first message without deleted filter
    Optional<Message> findFirstByConversationOrderBySentAtDesc(Conversation conversation);
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation AND m.deleted = false ORDER BY m.sentAt DESC LIMIT 1")
    Optional<Message> findFirstNonDeletedByConversationOrderBySentAtDesc(@Param("conversation") Conversation conversation);
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.sentAt < :beforeTimestamp AND m.deleted = false ORDER BY m.sentAt DESC")
    List<Message> findMessagesBefore(@Param("conversationId") Integer conversationId,
                                   @Param("beforeTimestamp") Instant beforeTimestamp,
                                   Pageable pageable);
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.sentAt > :afterTimestamp AND m.deleted = false ORDER BY m.sentAt ASC")
    List<Message> findMessagesAfter(@Param("conversationId") Integer conversationId,
                                  @Param("afterTimestamp") Instant afterTimestamp,
                                  Pageable pageable);
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.sentAt BETWEEN :startTime AND :endTime AND m.deleted = false ORDER BY m.sentAt ASC")
    List<Message> findMessagesBetween(@Param("conversationId") Integer conversationId,
                                    @Param("startTime") Instant startTime,
                                    @Param("endTime") Instant endTime);

    // ========== Entity Graph Methods ==========
    
    // ⚠️ WARNING: Fetches message with conversation without deleted filter
    @Query("SELECT m FROM Message m JOIN FETCH m.conversation WHERE m.id = :messageId")
    Optional<Message> findWithConversationById(@Param("messageId") Integer messageId);
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT m FROM Message m JOIN FETCH m.conversation WHERE m.id = :messageId AND m.deleted = false")
    Optional<Message> findNonDeletedWithConversationById(@Param("messageId") Integer messageId);
    
    // ⚠️ WARNING: Fetches message with conversation and author without deleted filter
    @Query("SELECT m FROM Message m JOIN FETCH m.conversation JOIN FETCH m.author WHERE m.id = :messageId")
    Optional<Message> findWithConversationAndAuthorById(@Param("messageId") Integer messageId);
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT m FROM Message m JOIN FETCH m.conversation JOIN FETCH m.author WHERE m.id = :messageId AND m.deleted = false")
    Optional<Message> findNonDeletedWithConversationAndAuthorById(@Param("messageId") Integer messageId);
    
    // ⚠️ WARNING: Fetches message with conversation and reply without deleted filter
    @Query("SELECT m FROM Message m JOIN FETCH m.conversation LEFT JOIN FETCH m.replyToMessage WHERE m.id = :messageId")
    Optional<Message> findWithConversationAndReplyById(@Param("messageId") Integer messageId);
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT m FROM Message m JOIN FETCH m.conversation LEFT JOIN FETCH m.replyToMessage WHERE m.id = :messageId AND m.deleted = false")
    Optional<Message> findNonDeletedWithConversationAndReplyById(@Param("messageId") Integer messageId);

    // ========== Search Queries ==========
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :conversationId AND LOWER(m.body) LIKE LOWER(CONCAT('%', :query, '%')) AND m.deleted = false")
    Page<Message> searchInConversation(@Param("conversationId") Integer conversationId, 
                                     @Param("query") String query, 
                                     Pageable pageable);
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT m FROM Message m JOIN m.conversation c JOIN c.participants cp WHERE cp.user.userId = :userId AND LOWER(m.body) LIKE LOWER(CONCAT('%', :query, '%')) AND m.deleted = false")
    Page<Message> searchInUserConversations(@Param("userId") Integer userId, 
                                          @Param("query") String query, 
                                          Pageable pageable);

    // ========== Reply/Thread Queries ==========
    
    // ⚠️ WARNING: Fetches ALL replies (including deleted)
    List<Message> findByReplyToMessage(Message parentMessage);
    
    // ✅ GOOD: Explicit deleted condition
    List<Message> findByReplyToMessageAndDeletedFalse(Message parentMessage);
    
    // ✅ GOOD: Explicit deleted condition
    Page<Message> findByReplyToMessageAndDeletedFalse(Message parentMessage, Pageable pageable);
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT m FROM Message m WHERE m.replyToMessage.id = :parentMessageId AND m.deleted = false")
    List<Message> findRepliesByParentId(@Param("parentMessageId") Integer parentMessageId);
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT COUNT(m) FROM Message m WHERE m.replyToMessage.id = :parentMessageId AND m.deleted = false")
    long countRepliesByParentId(@Param("parentMessageId") Integer parentMessageId);
    
    // ✅ GOOD: Explicit deleted condition
    long countByReplyToMessageAndDeletedFalse(Message replyToMessage);

    // ========== Pinned Messages ==========
    
    // ✅ GOOD: Explicit deleted condition
    List<Message> findByConversationAndPinnedTrueAndDeletedFalse(Conversation conversation);
    
    // ✅ GOOD: Explicit deleted condition
    List<Message> findByConversationAndPinnedTrueAndDeletedFalseOrderByPinnedAtDesc(Conversation conversation);
    
    // ✅ GOOD: Explicit deleted condition
    List<Message> findByConversationAndPinnedTrueAndPinnedAtAfterAndDeletedFalseOrderByPinnedAtDesc(
        Conversation conversation, 
        Instant pinnedAtAfter
    );
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.pinned = true AND m.deleted = false ORDER BY m.sentAt DESC")
    List<Message> findPinnedMessages(@Param("conversationId") Integer conversationId);
    
    // ✅ GOOD: Explicit deleted condition
    long countByConversationAndPinnedTrueAndDeletedFalse(Conversation conversation);
    
    // ✅ GOOD: Explicit deleted condition
    List<Message> findByConversation_ConversationIdInAndPinnedTrueAndDeletedFalse(
        @Param("conversationIds") List<Integer> conversationIds
    );

    // ========== Unread/Read Status Queries ==========
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.sentAt > :lastReadTimestamp AND m.deleted = false")
    long countUnreadMessages(@Param("conversationId") Integer conversationId, 
                           @Param("lastReadTimestamp") Instant lastReadTimestamp);
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.sentAt > :sinceTimestamp AND m.deleted = false")
    List<Message> findMessagesSince(@Param("conversationId") Integer conversationId, 
                                  @Param("sinceTimestamp") Instant sinceTimestamp);

    // ========== Statistics ==========
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.deleted = false")
    long countByConversationId(@Param("conversationId") Integer conversationId);
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT COUNT(m) FROM Message m WHERE m.author.userId = :userId AND m.deleted = false")
    long countByUserId(@Param("userId") Integer userId);
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.author.userId = :userId AND m.deleted = false")
    long countByConversationAndUser(@Param("conversationId") Integer conversationId, 
                                  @Param("userId") Integer userId);
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.sentAt >= :startDate AND m.sentAt < :endDate AND m.deleted = false")
    long countMessagesInPeriod(@Param("conversationId") Integer conversationId,
                             @Param("startDate") Instant startDate,
                             @Param("endDate") Instant endDate);

    // ========== Update Operations ==========
    
    // ✅ GOOD: Update operations don't need deleted filters
    @Modifying
    @Query("UPDATE Message m SET m.deleted = true, m.deletedAt = :deletedAt, m.deletionReason = :reason WHERE m.id = :messageId")
    void softDeleteMessage(@Param("messageId") Integer messageId, 
                         @Param("deletedAt") Instant deletedAt, 
                         @Param("reason") String reason);
    
    // ✅ GOOD: Update operations don't need deleted filters
    @Modifying
    @Query("UPDATE Message m SET m.pinned = :pinned WHERE m.id = :messageId")
    void updatePinnedStatus(@Param("messageId") Integer messageId, 
                          @Param("pinned") boolean pinned);
    
    // ✅ GOOD: Update operations don't need deleted filters
    @Modifying
    @Query("UPDATE Message m SET m.body = :body, m.edited = true, m.editedAt = :editedAt, m.editCount = m.editCount + 1 WHERE m.id = :messageId")
    void updateMessageContent(@Param("messageId") Integer messageId, 
                            @Param("body") String body, 
                            @Param("editedAt") Instant editedAt);

    // ========== Bulk Operations ==========
    
    // ✅ GOOD: Bulk update operations
    @Modifying
    @Query("UPDATE Message m SET m.deleted = true, m.deletedAt = :deletedAt WHERE m.conversation.conversationId = :conversationId")
    void softDeleteAllInConversation(@Param("conversationId") Integer conversationId, 
                                   @Param("deletedAt") Instant deletedAt);
    
    // ✅ GOOD: Bulk update operations
    @Modifying
    @Query("UPDATE Message m SET m.pinned = false WHERE m.conversation.conversationId = :conversationId")
    void unpinAllInConversation(@Param("conversationId") Integer conversationId);
    
    // ========== Existence Checks ==========
    
    // ⚠️ WARNING: Checks existence without deleted status
    boolean existsById(Integer messageId);
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Message m WHERE m.id = :messageId AND m.deleted = false")
    boolean existsNonDeletedById(@Param("messageId") Integer messageId);
    
    // ========== Additional Pinned Message Queries ==========
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation AND m.pinned = true AND m.pinnedAt > :pinnedAtAfter AND m.deleted = false ORDER BY m.pinnedAt DESC")
    List<Message> findRecentPinnedMessages(@Param("conversation") Conversation conversation, 
                                         @Param("pinnedAtAfter") Instant pinnedAtAfter);
    
    // ✅ GOOD: Explicit deleted condition
    @Query("SELECT COUNT(m) FROM Message m WHERE m.replyToMessage = :replyToMessage AND m.deleted = false")
    long countReplies(@Param("replyToMessage") Message replyToMessage);

    // ========== ADDITIONAL NON-DELETED METHODS ==========
    
    // Count methods for non-deleted messages
    long countByConversationAndDeletedFalse(Conversation conversation);
    long countByAuthor_UserIdAndDeletedFalse(Integer userId);
    long countByMessageTypeAndDeletedFalse(MessageType messageType);
    
    // Find methods with ordering for non-deleted messages
    List<Message> findByConversationAndDeletedFalseOrderBySentAtAsc(Conversation conversation);
    List<Message> findByConversationAndDeletedFalseOrderBySentAtDesc(Conversation conversation);
    Page<Message> findByConversationAndDeletedFalseOrderBySentAtDesc(Conversation conversation, Pageable pageable);
    
    // Find by conversation with different message types (non-deleted)
    List<Message> findByConversationAndMessageTypeAndDeletedFalseOrderBySentAtDesc(
        Conversation conversation, 
        MessageType messageType
    );
    
    // Find recent messages across multiple conversations (non-deleted)
    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId IN :conversationIds AND m.deleted = false ORDER BY m.sentAt DESC")
    List<Message> findRecentMessagesByConversationIds(
        @Param("conversationIds") List<Integer> conversationIds, 
        Pageable pageable
    );
    
    Optional<Message> findNonDeletedById(Integer id);

    // ========== PROJECTION-BASED QUERIES ==========

    // ✅ For message lists (conversation view)
    @Query("SELECT m.id as id, m.body as body, m.author.userId as authorId, " +
           "m.author.username as authorUsername, m.conversation.conversationId as conversationId, " +
           "m.sentAt as sentAt, m.edited as isEdited, m.messageType as messageType, " +
           "m.pinned as isPinned, m.replyToMessage.id as replyToMessageId, " +
           "CASE WHEN m.replyToMessage IS NOT NULL THEN m.replyToMessage.body ELSE NULL END as replyToMessageBody, " +
           "CASE WHEN m.replyToMessage IS NOT NULL THEN m.replyToMessage.author.userId ELSE NULL END as replyToAuthorId, " +
           "CASE WHEN m.replyToMessage IS NOT NULL THEN m.replyToMessage.author.username ELSE NULL END as replyToAuthorUsername " +
           "FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.deleted = false " +
           "ORDER BY m.sentAt DESC")
    Page<MessageListProjection> findMessageProjectionsByConversation(
        @Param("conversationId") Integer conversationId, 
        Pageable pageable
    );

    // ✅ For search results
    @Query("SELECT m.id as id, m.body as body, m.author.userId as authorId, " +
           "m.author.username as authorUsername, m.conversation.conversationId as conversationId, " +
           "m.sentAt as sentAt, m.edited as isEdited, m.messageType as messageType, " +
           "m.pinned as isPinned FROM Message m " +
           "WHERE m.conversation.conversationId = :conversationId " +
           "AND LOWER(m.body) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "AND m.deleted = false")
    Page<MessageListProjection> searchMessageProjectionsInConversation(
        @Param("conversationId") Integer conversationId, 
        @Param("query") String query, 
        Pageable pageable
    );

    // ✅ For light message lists (dashboard, notifications)
    @Query("SELECT m.id as id, m.body as body, m.author.userId as authorId, " +
           "m.author.username as authorUsername, m.conversation.conversationId as conversationId, " +
           "m.sentAt as sentAt, m.edited as isEdited, m.messageType as messageType, " +
           "m.pinned as isPinned FROM Message m " +
           "WHERE m.conversation.conversationId IN :conversationIds " +
           "AND m.deleted = false ORDER BY m.sentAt DESC")
    Page<MessageLightProjection> findLightMessageProjectionsByConversationIds(
        @Param("conversationIds") List<Integer> conversationIds, 
        Pageable pageable
    );

    // ========== ENTITY GRAPH METHODS FOR N+1 PREVENTION ==========

    @EntityGraph(attributePaths = {"author", "conversation"})
    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.deleted = false")
    Page<Message> findByConversationWithAuthor(@Param("conversationId") Integer conversationId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "conversation", "replyToMessage"})
    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.deleted = false")
    List<Message> findByConversationWithAuthorAndReply(@Param("conversationId") Integer conversationId);

    @EntityGraph(attributePaths = {"author", "conversation", "replyToMessage", "replyToMessage.author"})
    @Query("SELECT m FROM Message m WHERE m.id IN :messageIds AND m.deleted = false")
    List<Message> findByIdsWithFullData(@Param("messageIds") List<Integer> messageIds);

    // ========== BATCH FETCHING FOR REACTIONS ==========

    @Query("SELECT m FROM Message m LEFT JOIN FETCH m.reactions r LEFT JOIN FETCH r.user " +
           "WHERE m.conversation.conversationId = :conversationId AND m.deleted = false " +
           "AND m.sentAt BETWEEN :startTime AND :endTime")
    List<Message> findMessagesWithReactionsInPeriod(
        @Param("conversationId") Integer conversationId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    // ========== OPTIMIZED COUNT QUERIES ==========

    @Query(value = "SELECT COUNT(m) FROM messages m WHERE m.conversation_id = :conversationId AND m.deleted = false", 
           nativeQuery = true)
    long countNonDeletedByConversationIdNative(@Param("conversationId") Integer conversationId);

    // ========== PAGINATION HELPERS ==========

    @Query("SELECT m.id FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.deleted = false ORDER BY m.sentAt DESC")
    Page<Integer> findMessageIdsByConversation(@Param("conversationId") Integer conversationId, Pageable pageable);

    @Query("SELECT MIN(m.sentAt) FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.deleted = false")
    Optional<Instant> findOldestMessageTime(@Param("conversationId") Integer conversationId);

    @Query("SELECT MAX(m.sentAt) FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.deleted = false")
    Optional<Instant> findLatestMessageTime(@Param("conversationId") Integer conversationId);

    // ========== PINNED MESSAGE PROJECTIONS ==========

    // ✅ OPTIMIZED: For ConversationDetailDto - fetch only pinned message projections
    @Query("""
        SELECT
            m.id as id,
            m.body as body,
            m.sentAt as sentAt,
            m.messageType as messageType,
            m.author.userId as authorId,
            m.author.username as authorUsername,
            m.pinnedAt as pinnedAt,
            m.pinnedBy.userId as pinnedByUserId,
            m.pinnedBy.username as pinnedByUsername,
            m.replyToMessage.id as replyToMessageId,
            CASE WHEN m.replyToMessage IS NOT NULL THEN m.replyToMessage.body ELSE NULL END as replyToMessageBody,
            CASE WHEN m.replyToMessage IS NOT NULL THEN m.replyToMessage.author.userId ELSE NULL END as replyToAuthorId,
            CASE WHEN m.replyToMessage IS NOT NULL THEN m.replyToMessage.author.username ELSE NULL END as replyToAuthorUsername
        FROM Message m
        WHERE m.conversation.conversationId = :conversationId
        AND m.pinned = true
        AND m.deleted = false
        ORDER BY m.pinnedAt DESC
        """)
    List<PinnedMessageProjection> findPinnedMessageProjections(@Param("conversationId") Integer conversationId);
}