package com.tuniv.backend.qa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.model.QuestionVote;

@Repository
public interface QuestionVoteRepository extends JpaRepository<QuestionVote, QuestionVote.QuestionVoteId> {
}