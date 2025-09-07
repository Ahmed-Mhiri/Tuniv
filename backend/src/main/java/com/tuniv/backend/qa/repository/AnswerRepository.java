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

    List<Answer> findByAuthorUserIdOrderByCreatedAtDesc(Integer userId);
    List<Answer> findByQuestionAuthorUserIdAndIsSolutionTrueOrderByUpdatedAtDesc(Integer userId);

    // --- ⬇️ NEW OPTIMIZED METHOD TO ADD ⬇️ ---

    /**
     * Fetches all answers for a given list of question IDs.
     * It also eagerly fetches the answer's author and attachments to prevent N+1 calls.
     * This is a key batch-fetching method.
     */
    @Query("SELECT a FROM Answer a " +
           "JOIN FETCH a.author " +
           "LEFT JOIN FETCH a.attachments " +
           "WHERE a.question.id IN :questionIds")
    List<Answer> findAllByQuestionIdsWithDetails(@Param("questionIds") List<Integer> questionIds);


    @Query("SELECT a FROM Answer a JOIN FETCH a.author WHERE a.id = :answerId")
    Optional<Answer> findWithAuthorById(@Param("answerId") Integer answerId);

}