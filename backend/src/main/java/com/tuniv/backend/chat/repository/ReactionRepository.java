package com.tuniv.backend.chat.repository;

import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.model.Reaction;
import com.tuniv.backend.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Integer>, JpaSpecificationExecutor<Reaction> {

    // ========== Basic Finders ==========
    
    List<Reaction> findByMessage(Message message);
    
    List<Reaction> findByMessageAndIsRemovedFalse(Message message);
    
    List<Reaction> findByUser(User user);
    
    List<Reaction> findByUserAndIsRemovedFalse(User user);
    
    // ✅ CORRECTED: Method now matches the new unique constraint
    Optional<Reaction> findByMessageAndUserAndEmojiAndIsRemovedFalse(Message message, User user, String emoji);

    // ✅ RENAMED: More descriptive name for finding all reactions by a user on a message
    List<Reaction> findAllByMessageAndUserAndIsRemovedFalse(Message message, User user);

    // ========== Emoji-Based Queries ==========
    
    List<Reaction> findByMessageAndEmoji(Message message, String emoji);
    
    List<Reaction> findByMessageAndEmojiAndIsRemovedFalse(Message message, String emoji);
    
    @Query("SELECT r.emoji, COUNT(r) FROM Reaction r WHERE r.message = :message AND r.isRemoved = false GROUP BY r.emoji")
    List<Object[]> countReactionsByEmoji(@Param("message") Message message);
    
    @Query("SELECT r.emoji FROM Reaction r WHERE r.message = :message AND r.isRemoved = false GROUP BY r.emoji ORDER BY COUNT(r) DESC")
    List<String> findPopularEmojisByMessage(@Param("message") Message message, org.springframework.data.domain.Pageable pageable);

    // ========== Statistics ==========
    
    long countByMessageAndIsRemovedFalse(Message message);
    
    long countByUserAndIsRemovedFalse(User user);
    
    @Query("SELECT COUNT(r) FROM Reaction r WHERE r.message.id = :messageId AND r.isRemoved = false")
    long countActiveReactionsByMessageId(@Param("messageId") Integer messageId);

    // ========== Existence Checks ==========
    
    boolean existsByMessageAndUserAndEmojiAndIsRemovedFalse(Message message, User user, String emoji);

    // ========== Update Operations ==========
    
    @Modifying
    @Query("UPDATE Reaction r SET r.isRemoved = true, r.removedAt = CURRENT_TIMESTAMP WHERE r.message = :message AND r.user = :user AND r.emoji = :emoji")
    void removeSpecificReaction(@Param("message") Message message, 
                              @Param("user") User user, 
                              @Param("emoji") String emoji);

    // ========== Bulk Operations ==========
    
    @Query("SELECT r FROM Reaction r WHERE r.message.id IN :messageIds AND r.isRemoved = false")
    List<Reaction> findByMessage_IdInAndIsRemovedFalse(@Param("messageIds") List<Integer> messageIds);
}