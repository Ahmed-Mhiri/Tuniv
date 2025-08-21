package com.tuniv.backend.qa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.model.AnswerVote;

@Repository
public interface AnswerVoteRepository extends JpaRepository<AnswerVote, AnswerVote.AnswerVoteId> {
}