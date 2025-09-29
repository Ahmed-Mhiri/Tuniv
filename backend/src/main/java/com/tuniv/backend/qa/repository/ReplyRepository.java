package com.tuniv.backend.qa.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.model.Reply;

@Repository
public interface ReplyRepository extends JpaRepository<Reply, Integer> {

    long countByAuthor_UserId(Integer userId);

    List<Reply> findByAuthor_UserIdOrderByCreatedAtDesc(Integer userId);

    @Query("SELECT r FROM Reply r " +
           "JOIN FETCH r.author " +
           "LEFT JOIN FETCH r.attachments " +
           "WHERE r.topic.id = :topicId AND r.parentReply IS NULL")
    List<Reply> findTopLevelByTopicIdWithDetails(@Param("topicId") Integer topicId);

    @Query("SELECT r FROM Reply r JOIN FETCH r.author WHERE r.id = :replyId")
    Optional<Reply> findWithAuthorById(@Param("replyId") Integer replyId);

    @Query("SELECT r FROM Reply r JOIN FETCH r.topic WHERE r.id = :replyId")
    Optional<Reply> findWithTopicById(@Param("replyId") Integer replyId);

    @Query("SELECT r FROM Reply r " +
           "JOIN FETCH r.author " +
           "JOIN FETCH r.topic t " +
           "JOIN FETCH t.author " +
           "WHERE r.id = :replyId")
    Optional<Reply> findWithDetailsById(@Param("replyId") Integer replyId);

    @Query("SELECT r.topic.id FROM Reply r WHERE r.id = :replyId")
    Optional<Integer> findTopicIdById(@Param("replyId") Integer replyId);


    // ✅ NEW: Count only answers (replies on QUESTION topics that are top-level)
    @Query("SELECT COUNT(r) FROM Reply r WHERE r.author.userId = :userId AND r.topic.topicType = com.tuniv.backend.qa.model.TopicType.QUESTION AND r.parentReply IS NULL")
    long countAnswersByUser(@Param("userId") Integer userId);

    // ✅ NEW: Get only answers
    @Query("SELECT r FROM Reply r WHERE r.author.userId = :userId AND r.topic.topicType = com.tuniv.backend.qa.model.TopicType.QUESTION AND r.parentReply IS NULL ORDER BY r.createdAt DESC")
    List<Reply> findAnswersByUser(@Param("userId") Integer userId);

    // ✅ NEW: Get only comments (replies on POST topics OR nested replies)
    @Query("SELECT r FROM Reply r WHERE r.author.userId = :userId AND (r.topic.topicType = com.tuniv.backend.qa.model.TopicType.POST OR r.parentReply IS NOT NULL) ORDER BY r.createdAt DESC")
    List<Reply> findCommentsByUser(@Param("userId") Integer userId);

    // ✅ NEW: Find only answers (top-level replies on QUESTION topics)
       @Query("SELECT r FROM Reply r WHERE r.topic.id = :topicId AND r.topic.topicType = com.tuniv.backend.qa.model.TopicType.QUESTION AND r.parentReply IS NULL ORDER BY r.createdAt ASC")
       List<Reply> findAnswersByTopicId(@Param("topicId") Integer topicId);

       // ✅ NEW: Find only comments (replies on POST topics OR nested replies)
       @Query("SELECT r FROM Reply r WHERE r.topic.id = :topicId AND (r.topic.topicType = com.tuniv.backend.qa.model.TopicType.POST OR r.parentReply IS NOT NULL) ORDER BY r.createdAt ASC")
       List<Reply> findCommentsByTopicId(@Param("topicId") Integer topicId);

       // ✅ NEW: Find nested comments for a specific parent reply
       List<Reply> findByParentReplyIdOrderByCreatedAtAsc(Integer parentReplyId);
}
