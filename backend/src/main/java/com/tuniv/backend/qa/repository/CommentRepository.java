package com.tuniv.backend.qa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.model.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {

    List<Comment> findByAnswerIdOrderByCreatedAtAsc(Integer answerId);
    List<Comment> findByAnswerIdAndParentCommentIsNullOrderByCreatedAtAsc(Integer answerId);
    List<Comment> findByAuthor_UserIdOrderByCreatedAtDesc(Integer userId);

    @Query("SELECT c FROM Comment c " +
           "JOIN FETCH c.author " +
           "LEFT JOIN FETCH c.attachments " +
           "WHERE c.answer.id IN :answerIds AND c.parentComment IS NULL")
    List<Comment> findTopLevelByAnswerIdsWithDetails(@Param("answerIds") List<Integer> answerIds);

    @Query("SELECT c FROM Comment c JOIN FETCH c.author WHERE c.id = :commentId")
    Optional<Comment> findWithAuthorById(@Param("commentId") Integer commentId);

    @Query("SELECT c FROM Comment c JOIN FETCH c.answer a JOIN FETCH a.question WHERE c.id = :commentId")
    Optional<Comment> findWithParentsById(@Param("commentId") Integer commentId);

    @Query("SELECT c.answer.question.id FROM Comment c WHERE c.id = :commentId")
    Optional<Integer> findQuestionIdById(@Param("commentId") Integer commentId);
}