package com.tuniv.backend.qa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.model.Reply;
import com.tuniv.backend.qa.model.TopicType;

@Repository
public interface ReplyRepository extends JpaRepository<Reply, Integer> {

    long countByAuthor_UserId(Integer userId);

    // ✅ NEW: Find replies by topic ID with parent replies (comments only) with pagination
       @EntityGraph(attributePaths = {"author"})
       Page<Reply> findByTopicIdAndParentReplyIsNotNull(Integer topicId, Pageable pageable);
    // ✅ NEW: Find all replies with pagination and ordering
       @EntityGraph(attributePaths = {"author", "topic"})
       Page<Reply> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Reply> findByAuthor_UserIdOrderByCreatedAtDesc(Integer userId);

    // ✅ OPTIMIZED: Replaced JOIN FETCH with EntityGraph for consistency
    @EntityGraph(attributePaths = {"author", "attachments"})
    List<Reply> findByTopicIdAndParentReplyIsNull(@Param("topicId") Integer topicId);

    // ✅ OPTIMIZED: Using EntityGraph instead of JOIN FETCH
    @EntityGraph(attributePaths = {"author"})
    Optional<Reply> findWithAuthorById(@Param("replyId") Integer replyId);

    // ✅ OPTIMIZED: Using EntityGraph instead of JOIN FETCH
    @EntityGraph(attributePaths = {"topic"})
    Optional<Reply> findWithTopicById(@Param("replyId") Integer replyId);

    // ✅ OPTIMIZED: Using EntityGraph instead of JOIN FETCH
    @EntityGraph(attributePaths = {"author", "topic", "topic.author"})
    Optional<Reply> findWithDetailsById(@Param("replyId") Integer replyId);

    @Query("SELECT r.topic.id FROM Reply r WHERE r.id = :replyId")
    Optional<Integer> findTopicIdById(@Param("replyId") Integer replyId);

    // ✅ NEW: Count only answers (replies on QUESTION topics that are top-level)
    @Query("SELECT COUNT(r) FROM Reply r WHERE r.author.userId = :userId AND r.topic.topicType = com.tuniv.backend.qa.model.TopicType.QUESTION AND r.parentReply IS NULL")
    long countAnswersByUser(@Param("userId") Integer userId);

    // ✅ NEW: Count comments by user
    @Query("SELECT COUNT(r) FROM Reply r WHERE r.author.userId = :userId AND (r.topic.topicType = com.tuniv.backend.qa.model.TopicType.POST OR r.parentReply IS NOT NULL)")
    long countCommentsByUser(@Param("userId") Integer userId);

    // ✅ NEW: Get only answers with pagination
    @EntityGraph(attributePaths = {"author", "topic"})
    Page<Reply> findByAuthorUserIdAndTopicTopicTypeAndParentReplyIsNull(
        @Param("userId") Integer userId, 
        TopicType topicType, 
        Pageable pageable
    );

    // ✅ NEW: Get only answers
    @EntityGraph(attributePaths = {"author", "topic"})
    List<Reply> findAnswersByUser(@Param("userId") Integer userId);

    // ✅ NEW: Get only comments (replies on POST topics OR nested replies)
    @EntityGraph(attributePaths = {"author", "topic"})
    @Query("SELECT r FROM Reply r WHERE r.author.userId = :userId AND (r.topic.topicType = com.tuniv.backend.qa.model.TopicType.POST OR r.parentReply IS NOT NULL) ORDER BY r.createdAt DESC")
    List<Reply> findCommentsByUser(@Param("userId") Integer userId);

    // ✅ NEW: Find only answers (top-level replies on QUESTION topics)
    @EntityGraph(attributePaths = {"author"})
    List<Reply> findAnswersByTopicId(@Param("topicId") Integer topicId);

    // ✅ NEW: Find only comments (replies on POST topics OR nested replies)
    @EntityGraph(attributePaths = {"author"})
    List<Reply> findCommentsByTopicId(@Param("topicId") Integer topicId);

    // ✅ NEW: Find nested comments for a specific parent reply
    @EntityGraph(attributePaths = {"author"})
    List<Reply> findByParentReplyIdOrderByCreatedAtAsc(Integer parentReplyId);

    // ✅ NEW: Batch operations for better performance
    @EntityGraph(attributePaths = {"author"})
    List<Reply> findByIdIn(List<Integer> replyIds);

    // ✅ NEW: Find replies with votes for multiple replies
    @Query("SELECT r.id, COUNT(v), SUM(CASE WHEN v.value > 0 THEN 1 ELSE 0 END) " +
           "FROM Reply r LEFT JOIN r.votes v WHERE r.id IN :replyIds GROUP BY r.id")
    List<Object[]> findVoteStatsByReplyIds(@Param("replyIds") List<Integer> replyIds);

    // ✅ NEW: Efficient pagination for topic replies
    @EntityGraph(attributePaths = {"author", "attachments"})
    Page<Reply> findByTopicIdAndParentReplyIsNull(Integer topicId, Pageable pageable);

    // ✅ NEW: Find all top-level replies for a topic with pagination
    @EntityGraph(attributePaths = {"author"})
    Page<Reply> findByTopicIdAndParentReplyIsNullOrderByCreatedAtAsc(Integer topicId, Pageable pageable);

    // ✅ NEW: Find child replies with pagination
    @EntityGraph(attributePaths = {"author"})
    Page<Reply> findByParentReplyIdOrderByCreatedAtAsc(Integer parentReplyId, Pageable pageable);

    // ✅ NEW: Count replies by topic (alternative to denormalized count)
    @Query("SELECT COUNT(r) FROM Reply r WHERE r.topic.id = :topicId AND r.parentReply IS NULL")
    long countTopLevelRepliesByTopicId(@Param("topicId") Integer topicId);

    // ✅ NEW: Count all replies (including nested) by topic
    @Query("SELECT COUNT(r) FROM Reply r WHERE r.topic.id = :topicId")
    long countAllRepliesByTopicId(@Param("topicId") Integer topicId);

    // ✅ NEW: Find recent replies with user details
    @EntityGraph(attributePaths = {"author", "topic"})
    List<Reply> findTop10ByOrderByCreatedAtDesc();

    // ✅ NEW: Find replies by multiple topics
    @EntityGraph(attributePaths = {"author"})
    List<Reply> findByTopicIdIn(List<Integer> topicIds);

    // ✅ NEW: Efficient deletion methods
    @Modifying
    @Query("DELETE FROM Reply r WHERE r.topic.id = :topicId")
    void deleteByTopicId(@Param("topicId") Integer topicId);

    @Modifying
    @Query("DELETE FROM Reply r WHERE r.parentReply.id = :parentReplyId")
    void deleteByParentReplyId(@Param("parentReplyId") Integer parentReplyId);

    // ✅ NEW: Find replies with high scores (popular replies)
    @EntityGraph(attributePaths = {"author", "topic"})
    List<Reply> findTop10ByOrderByScoreDesc();

    // ✅ NEW: Check if user has replied to a topic
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reply r WHERE r.topic.id = :topicId AND r.author.userId = :userId")
    boolean existsByTopicIdAndAuthorUserId(@Param("topicId") Integer topicId, @Param("userId") Integer userId);

    // ✅ NEW: Find user's reply to a specific topic
    @EntityGraph(attributePaths = {"author"})
    Optional<Reply> findByTopicIdAndAuthorUserId(@Param("topicId") Integer topicId, @Param("userId") Integer userId);

    // ✅ NEW: Find accepted solutions by user
    @EntityGraph(attributePaths = {"author", "topic"})
    @Query("SELECT r FROM Reply r WHERE r.topic.acceptedSolution.id = r.id AND r.author.userId = :userId")
    List<Reply> findAcceptedSolutionsByUser(@Param("userId") Integer userId);

    // ✅ NEW: Count accepted solutions by user
    @Query("SELECT COUNT(r) FROM Reply r WHERE r.topic.acceptedSolution.id = r.id AND r.author.userId = :userId")
    long countAcceptedSolutionsByUser(@Param("userId") Integer userId);
}