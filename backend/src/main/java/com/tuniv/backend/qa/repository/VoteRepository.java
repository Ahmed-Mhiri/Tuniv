package com.tuniv.backend.qa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tuniv.backend.qa.dto.VoteInfo;
import com.tuniv.backend.qa.model.Vote;


public interface VoteRepository extends JpaRepository<Vote, Integer> {

    /**
     * Finds an existing vote by a specific user on a specific post.
     * This is used by the VoteService.
     */
    Optional<Vote> findByUser_IdAndPost_Id(Integer userId, Integer postId);

    /**
     * ✅ ADD THIS METHOD
     * Fetches all votes cast by a specific user, ordered by most recent first.
     * This is required by the ActivityService.
     */
    List<Vote> findByUser_IdOrderByCreatedAtDesc(Integer userId);

    /**
     * Fetches all votes for a given user across multiple posts
     * in a single, highly efficient database query.
     */
    @Query("""
        SELECT new com.yourpackage.dto.VoteInfo(v.post.id, v.value)
        FROM Vote v
        WHERE v.user.id = :userId AND v.post.id IN :postIds
    """)
    List<VoteInfo> findAllVotesForUserByPostIds(
        @Param("userId") Integer userId,
        @Param("postIds") List<Integer> postIds
    );
}