package com.tuniv.backend.qa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.model.CommentVote;

@Repository
public interface CommentVoteRepository extends JpaRepository<CommentVote, CommentVote.CommentVoteId> {
}