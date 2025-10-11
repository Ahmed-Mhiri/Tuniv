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

    // ✅ Efficient count, but prefer the denormalized `replyCount` on the User entity for display.
    long countByAuthor_UserId(Integer userId);

    // ✅ Good query for fetching nested replies (comments on answers).
    @EntityGraph(attributePaths = {"author"})
    Page<Reply> findByTopicIdAndParentReplyIsNotNull(Integer topicId, Pageable pageable);

    // ✅ Good general-purpose paginated fetch.
    @EntityGraph(attributePaths = {"author", "topic"})
    Page<Reply> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // ✅ Standard query for finding a user's replies.
    List<Reply> findByAuthor_UserIdOrderByCreatedAtDesc(Integer userId);

    // ❌ REMOVED ATTRIBUTE: `attachments` no longer exists on the Post entity.
    @EntityGraph(attributePaths = {"author"})
    List<Reply> findByTopicIdAndParentReplyIsNull(@Param("topicId") Integer topicId);

    // ✅ Good, efficient fetch methods.
    @EntityGraph(attributePaths = {"author"})
    Optional<Reply> findWithAuthorById(@Param("replyId") Integer replyId);

    @EntityGraph(attributePaths = {"topic"})
    Optional<Reply> findWithTopicById(@Param("replyId") Integer replyId);

    @EntityGraph(attributePaths = {"author", "topic", "topic.author"})
    Optional<Reply> findWithDetailsById(@Param("replyId") Integer replyId);

    // ✅ Excellent performance pattern for getting just a foreign key.
    @Query("SELECT r.topic.id FROM Reply r WHERE r.id = :replyId")
    Optional<Integer> findTopicIdById(@Param("replyId") Integer replyId);

    // ✅ Defines an "answer" as a top-level reply to a QUESTION.
    @Query("SELECT COUNT(r) FROM Reply r WHERE r.author.userId = :userId AND r.topic.topicType = com.tuniv.backend.qa.model.TopicType.QUESTION AND r.parentReply IS NULL")
    long countAnswersByUser(@Param("userId") Integer userId);

    // 💡 LOGIC CORRECTION: Defines a "comment" as a nested reply OR a top-level reply on a non-question topic.
    @Query("SELECT COUNT(r) FROM Reply r WHERE r.author.userId = :userId AND (r.topic.topicType <> com.tuniv.backend.qa.model.TopicType.QUESTION OR r.parentReply IS NOT NULL)")
    long countCommentsByUser(@Param("userId") Integer userId);

    // ✅ Gets a paginated list of a user's answers.
    @EntityGraph(attributePaths = {"author", "topic"})
    Page<Reply> findByAuthorUserIdAndTopicTopicTypeAndParentReplyIsNull(
        @Param("userId") Integer userId,
        TopicType topicType, // Pass TopicType.QUESTION here from the service
        Pageable pageable
    );
    
    // 💡 LOGIC CORRECTION: Added a WHERE clause to properly define "answers".
    @EntityGraph(attributePaths = {"author", "topic"})
    @Query("SELECT r FROM Reply r WHERE r.author.userId = :userId AND r.topic.topicType = com.tuniv.backend.qa.model.TopicType.QUESTION AND r.parentReply IS NULL")
    List<Reply> findAnswersByUser(@Param("userId") Integer userId);
    
    // 💡 LOGIC CORRECTION: Corrected query to properly define "comments".
    @EntityGraph(attributePaths = {"author", "topic"})
    @Query("SELECT r FROM Reply r WHERE r.author.userId = :userId AND (r.topic.topicType <> com.tuniv.backend.qa.model.TopicType.QUESTION OR r.parentReply IS NOT NULL) ORDER BY r.createdAt DESC")
    List<Reply> findCommentsByUser(@Param("userId") Integer userId);
    
    // ✅ Defines an "answer" as a top-level reply on a QUESTION topic.
    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT r FROM Reply r WHERE r.topic.id = :topicId AND r.parentReply IS NULL AND r.topic.topicType = com.tuniv.backend.qa.model.TopicType.QUESTION")
    List<Reply> findAnswersByTopicId(@Param("topicId") Integer topicId);

    // ✅ Defines a "comment" as a nested reply on a QUESTION topic.
    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT r FROM Reply r WHERE r.topic.id = :topicId AND r.parentReply IS NOT NULL")
    List<Reply> findCommentsByTopicId(@Param("topicId") Integer topicId);

    // ✅ Perfect for fetching threaded replies.
    @EntityGraph(attributePaths = {"author"})
    List<Reply> findByParentReplyIdOrderByCreatedAtAsc(Integer parentReplyId);

    @EntityGraph(attributePaths = {"author"})
    List<Reply> findByIdIn(List<Integer> replyIds);

    /**
     * ✨ MASSIVELY IMPROVED: The old query was invalid and inefficient.
     * This now leverages the denormalized score, upvoteCount, and downvoteCount fields
     * directly on the Reply entity, which is extremely fast and avoids any joins to the Vote table.
     * The method returns a List of Object arrays, where each array is [Integer id, Integer score, Integer upvoteCount, Integer downvoteCount].
     */
    @Query("SELECT r.id, r.score, r.upvoteCount, r.downvoteCount FROM Reply r WHERE r.id IN :replyIds")
    List<Object[]> findVoteStatsByReplyIds(@Param("replyIds") List<Integer> replyIds);
    
    // ❌ REMOVED ATTRIBUTE: `attachments` no longer exists on the Post entity.
    @EntityGraph(attributePaths = {"author"})
    Page<Reply> findByTopicIdAndParentReplyIsNull(Integer topicId, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Page<Reply> findByTopicIdAndParentReplyIsNullOrderByCreatedAtAsc(Integer topicId, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Page<Reply> findByParentReplyIdOrderByCreatedAtAsc(Integer parentReplyId, Pageable pageable);

    // ✅ Good queries for data validation or recalculation jobs.
    @Query("SELECT COUNT(r) FROM Reply r WHERE r.topic.id = :topicId AND r.parentReply IS NULL")
    long countTopLevelRepliesByTopicId(@Param("topicId") Integer topicId);
    
    @Query("SELECT COUNT(r) FROM Reply r WHERE r.topic.id = :topicId")
    long countAllRepliesByTopicId(@Param("topicId") Integer topicId);

    // ✅ Good queries for dashboards or "latest activity" feeds.
    @EntityGraph(attributePaths = {"author", "topic"})
    List<Reply> findTop10ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"author", "topic"})
    List<Reply> findTop10ByOrderByScoreDesc();

    @EntityGraph(attributePaths = {"author"})
    List<Reply> findByTopicIdIn(List<Integer> topicIds);
    
    // ✅ Efficient bulk delete operations. Use with care as they bypass JPA lifecycle callbacks.
    @Modifying
    @Query("DELETE FROM Reply r WHERE r.topic.id = :topicId")
    void deleteByTopicId(@Param("topicId") Integer topicId);

    @Modifying
    @Query("DELETE FROM Reply r WHERE r.parentReply.id = :parentReplyId")
    void deleteByParentReplyId(@Param("parentReplyId") Integer parentReplyId);

    // ✅ Good, specific queries for checking user interactions.
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reply r WHERE r.topic.id = :topicId AND r.author.userId = :userId")
    boolean existsByTopicIdAndAuthorUserId(@Param("topicId") Integer topicId, @Param("userId") Integer userId);

    @EntityGraph(attributePaths = {"author"})
    Optional<Reply> findByTopicIdAndAuthorUserId(@Param("topicId") Integer topicId, @Param("userId") Integer userId);

    // ✅ Correct logic for finding replies that have been accepted as solutions.
    @EntityGraph(attributePaths = {"author", "topic"})
    @Query("SELECT r FROM Reply r WHERE r.topic.acceptedSolution.id = r.id AND r.author.userId = :userId")
    List<Reply> findAcceptedSolutionsByUser(@Param("userId") Integer userId);
    
    @Query("SELECT COUNT(r) FROM Reply r WHERE r.topic.acceptedSolution.id = r.id AND r.author.userId = :userId")
    long countAcceptedSolutionsByUser(@Param("userId") Integer userId);
}