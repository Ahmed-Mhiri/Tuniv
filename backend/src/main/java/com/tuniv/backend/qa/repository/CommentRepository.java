package com.tuniv.backend.qa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.model.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {
    // ✅ FIX: Changed from AnswerAnswerId to AnswerId
    List<Comment> findByAnswerIdOrderByCreatedAtAsc(Integer answerId);
    
    // ✅ FIX: Changed from AnswerAnswerId to AnswerId
    List<Comment> findByAnswerIdAndParentCommentIsNullOrderByCreatedAtAsc(Integer answerId);
    
    List<Comment> findByAuthorUserIdOrderByCreatedAtDesc(Integer userId);
}