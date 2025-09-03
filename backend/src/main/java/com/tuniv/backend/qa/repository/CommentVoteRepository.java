package com.tuniv.backend.qa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.model.CommentVote;

@Repository
public interface CommentVoteRepository extends JpaRepository<CommentVote, CommentVote.CommentVoteId> {
    List<CommentVote> findByUserUserIdOrderByCreatedAtDesc(Integer userId);

    // ✅ FIX: Changed from CommentCommentIdIn to CommentIdIn
    List<CommentVote> findByCommentIdIn(List<Integer> commentIds);
}