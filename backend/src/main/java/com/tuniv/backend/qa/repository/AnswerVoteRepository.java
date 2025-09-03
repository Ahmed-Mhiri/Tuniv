package com.tuniv.backend.qa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.model.AnswerVote;

@Repository
public interface AnswerVoteRepository extends JpaRepository<AnswerVote, AnswerVote.AnswerVoteId> {
    List<AnswerVote> findByUserUserIdOrderByCreatedAtDesc(Integer userId);
    List<AnswerVote> findByAnswerAnswerIdIn(List<Integer> answerIds);


}