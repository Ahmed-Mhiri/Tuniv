package com.tuniv.backend.qa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.model.Answer;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Integer> {

    List<Answer> findByAuthor_IdOrderByCreatedAtDesc(Integer userId);
    List<Answer> findByQuestion_Author_IdAndIsSolutionTrueOrderByUpdatedAtDesc(Integer userId);

    @Query("SELECT a FROM Answer a " +
           "JOIN FETCH a.author " +
           "LEFT JOIN FETCH a.attachments " +
           "WHERE a.question.id IN :questionIds")
    List<Answer> findAllByQuestionIdsWithDetails(@Param("questionIds") List<Integer> questionIds);

    @Query("SELECT a FROM Answer a JOIN FETCH a.author WHERE a.id = :answerId")
    Optional<Answer> findWithAuthorById(@Param("answerId") Integer answerId);

    @Query("SELECT a FROM Answer a JOIN FETCH a.question WHERE a.id = :answerId")
    Optional<Answer> findWithQuestionById(@Param("answerId") Integer answerId);

    @Query("SELECT a FROM Answer a " +
           "JOIN FETCH a.author " +
           "JOIN FETCH a.question q " +
           "JOIN FETCH q.author " +
           "WHERE a.id = :answerId")
    Optional<Answer> findWithDetailsById(@Param("answerId") Integer answerId);

    @Query("SELECT a FROM Answer a WHERE a.question.id = :questionId AND a.isSolution = true")
    Optional<Answer> findSolutionByQuestionId(@Param("questionId") Integer questionId);

    @Query("SELECT a.question.id FROM Answer a WHERE a.id = :answerId")
    Optional<Integer> findQuestionIdById(@Param("answerId") Integer answerId);
}