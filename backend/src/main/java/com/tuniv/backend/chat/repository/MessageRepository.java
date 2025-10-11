package com.tuniv.backend.chat.repository;

import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.model.MessageType;
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
public interface MessageRepository extends JpaRepository<Message, Integer>, JpaSpecificationExecutor<Message> {

    // ========== Basic Finders ==========
    
    List<Message> findByConversation(Conversation conversation);
    
    Page<Message> findByConversation(Conversation conversation, Pageable pageable);
    
    List<Message> findByConversationAndDeletedFalse(Conversation conversation);
    
    Page<Message> findByConversationAndDeletedFalse(Conversation conversation, Pageable pageable);
    
    List<Message> findByAuthor_UserId(Integer userId);
    
    Page<Message> findByAuthor_UserId(Integer userId, Pageable pageable);
    
    List<Message> findByMessageType(MessageType messageType);
    
    List<Message> findByConversationAndMessageType(Conversation conversation, MessageType messageType);

    // ========== Complex Queries ==========
    
    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.deleted = false ORDER BY m.sentAt DESC")
    List<Message> findLatestMessagesByConversation(@Param("conversationId") Integer conversationId, Pageable pageable);
    
    Optional<Message> findFirstByConversationOrderBySentAtDesc(Conversation conversation);
    
    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.sentAt < :beforeTimestamp AND m.deleted = false ORDER BY m.sentAt DESC")
    List<Message> findMessagesBefore(@Param("conversationId") Integer conversationId,
                                   @Param("beforeTimestamp") Instant beforeTimestamp,
                                   Pageable pageable);
    
    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.sentAt > :afterTimestamp AND m.deleted = false ORDER BY m.sentAt ASC")
    List<Message> findMessagesAfter(@Param("conversationId") Integer conversationId,
                                  @Param("afterTimestamp") Instant afterTimestamp,
                                  Pageable pageable);
    
    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.sentAt BETWEEN :startTime AND :endTime AND m.deleted = false ORDER BY m.sentAt ASC")
    List<Message> findMessagesBetween(@Param("conversationId") Integer conversationId,
                                    @Param("startTime") Instant startTime,
                                    @Param("endTime") Instant endTime);

    // ========== Search Queries ==========
    
    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :conversationId AND LOWER(m.body) LIKE LOWER(CONCAT('%', :query, '%')) AND m.deleted = false")
    Page<Message> searchInConversation(@Param("conversationId") Integer conversationId, 
                                     @Param("query") String query, 
                                     Pageable pageable);
    
    @Query("SELECT m FROM Message m JOIN m.conversation c JOIN c.participants cp WHERE cp.user.userId = :userId AND LOWER(m.body) LIKE LOWER(CONCAT('%', :query, '%')) AND m.deleted = false")
    Page<Message> searchInUserConversations(@Param("userId") Integer userId, 
                                          @Param("query") String query, 
                                          Pageable pageable);

    // ========== Reply/Thread Queries ==========
    
    List<Message> findByReplyToMessage(Message parentMessage);
    
    List<Message> findByReplyToMessageAndDeletedFalse(Message parentMessage);
    
    Page<Message> findByReplyToMessageAndDeletedFalse(Message parentMessage, Pageable pageable);
    
    @Query("SELECT m FROM Message m WHERE m.replyToMessage.id = :parentMessageId AND m.deleted = false")
    List<Message> findRepliesByParentId(@Param("parentMessageId") Integer parentMessageId);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.replyToMessage.id = :parentMessageId AND m.deleted = false")
    long countRepliesByParentId(@Param("parentMessageId") Integer parentMessageId);

    // ========== Pinned Messages ==========
    
    List<Message> findByConversationAndPinnedTrueAndDeletedFalse(Conversation conversation);
    
    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.pinned = true AND m.deleted = false ORDER BY m.sentAt DESC")
    List<Message> findPinnedMessages(@Param("conversationId") Integer conversationId);
    
    long countByConversationAndPinnedTrueAndDeletedFalse(Conversation conversation);

    // ========== Unread/Read Status Queries ==========
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.sentAt > :lastReadTimestamp AND m.deleted = false")
    long countUnreadMessages(@Param("conversationId") Integer conversationId, 
                           @Param("lastReadTimestamp") Instant lastReadTimestamp);
    
    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.sentAt > :sinceTimestamp AND m.deleted = false")
    List<Message> findMessagesSince(@Param("conversationId") Integer conversationId, 
                                  @Param("sinceTimestamp") Instant sinceTimestamp);

    // ========== Statistics ==========
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.deleted = false")
    long countByConversationId(@Param("conversationId") Integer conversationId);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.author.userId = :userId AND m.deleted = false")
    long countByUserId(@Param("userId") Integer userId);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.author.userId = :userId AND m.deleted = false")
    long countByConversationAndUser(@Param("conversationId") Integer conversationId, 
                                  @Param("userId") Integer userId);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.conversationId = :conversationId AND m.sentAt >= :startDate AND m.sentAt < :endDate AND m.deleted = false")
    long countMessagesInPeriod(@Param("conversationId") Integer conversationId,
                             @Param("startDate") Instant startDate,
                             @Param("endDate") Instant endDate);

    // ========== Update Operations ==========
    
    @Modifying
    @Query("UPDATE Message m SET m.deleted = true, m.deletedAt = :deletedAt, m.deletionReason = :reason WHERE m.id = :messageId")
    void softDeleteMessage(@Param("messageId") Integer messageId, 
                         @Param("deletedAt") Instant deletedAt, 
                         @Param("reason") String reason);
    
    @Modifying
    @Query("UPDATE Message m SET m.pinned = :pinned WHERE m.id = :messageId")
    void updatePinnedStatus(@Param("messageId") Integer messageId, 
                          @Param("pinned") boolean pinned);
    
    @Modifying
    @Query("UPDATE Message m SET m.body = :body, m.edited = true, m.editedAt = :editedAt, m.editCount = m.editCount + 1 WHERE m.id = :messageId")
    void updateMessageContent(@Param("messageId") Integer messageId, 
                            @Param("body") String body, 
                            @Param("editedAt") Instant editedAt);

    // ========== Bulk Operations ==========
    
    @Modifying
    @Query("UPDATE Message m SET m.deleted = true, m.deletedAt = :deletedAt WHERE m.conversation.conversationId = :conversationId")
    void softDeleteAllInConversation(@Param("conversationId") Integer conversationId, 
                                   @Param("deletedAt") Instant deletedAt);
    
    @Modifying
    @Query("UPDATE Message m SET m.pinned = false WHERE m.conversation.conversationId = :conversationId")
    void unpinAllInConversation(@Param("conversationId") Integer conversationId);
}