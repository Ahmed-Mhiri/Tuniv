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
     * ✅ RENAMED: Changed from User_Id to User_UserId to match the User entity.
     */
    Optional<Vote> findByUser_UserIdAndPost_Id(Integer userId, Integer postId);

    /**
     * ✅ RENAMED: Changed from User_Id to User_UserId to match the User entity.
     */
    List<Vote> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);

    /**
     * ✅ UPDATED: Changed from v.user.id to v.user.userId in the WHERE clause.
     */
    @Query("""
        SELECT new com.tuniv.backend.qa.dto.VoteInfo(v.post.id, v.value)
        FROM Vote v
        WHERE v.user.userId = :userId AND v.post.id IN :postIds
    """)
    List<VoteInfo> findAllVotesForUserByPostIds(
        @Param("userId") Integer userId,
        @Param("postIds") List<Integer> postIds
    );
}