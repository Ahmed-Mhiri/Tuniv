package com.tuniv.backend.chat.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tuniv.backend.chat.model.Reaction;

public interface ReactionRepository extends JpaRepository<Reaction, Integer> {

    /**
     * ✅ RENAMED: Changed from User_Id to User_UserId to match the User entity.
     */
    Optional<Reaction> findByUser_UserIdAndPost_IdAndEmoji(Integer userId, Integer postId, String emoji);

    /**
     * Finds all reactions for a given post.
     */
    List<Reaction> findByPost_Id(Integer postId);

    /**
     * Finds all reactions for a given list of post IDs.
     */
    List<Reaction> findByPost_IdIn(List<Integer> postIds);
}