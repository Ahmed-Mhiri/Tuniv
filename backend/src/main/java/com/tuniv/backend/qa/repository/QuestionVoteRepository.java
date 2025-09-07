package com.tuniv.backend.qa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.model.QuestionVote;

@Repository
public interface QuestionVoteRepository extends JpaRepository<QuestionVote, QuestionVote.QuestionVoteId> {
    List<QuestionVote> findByUserUserIdOrderByCreatedAtDesc(Integer userId);

    // ✅ FIX: Changed from QuestionQuestionIdIn to QuestionIdIn
    List<QuestionVote> findByQuestionIdIn(List<Integer> questionIds);

    List<QuestionVote> findByUserIdAndQuestionIdIn(Integer userId, List<Integer> questionIds);

}