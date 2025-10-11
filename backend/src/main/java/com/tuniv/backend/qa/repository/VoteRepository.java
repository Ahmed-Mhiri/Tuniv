package com.tuniv.backend.qa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.dto.VoteInfo;
import com.tuniv.backend.qa.model.Vote;


@Repository
public interface VoteRepository extends JpaRepository<Vote, Integer> {

    /**
     * Finds the active vote for a given user on a specific post.
     * This is the primary method to check if a user has already voted.
     */
    Optional<Vote> findByUser_UserIdAndPost_Id(Integer userId, Integer postId);

    /**
     * Finds a user's entire voting history.
     */
    List<Vote> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);

    /**
     * An efficient DTO projection to get a user's vote status for a list of posts
     * in a single query. Perfect for hydrating a feed of topics or replies.
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
    
    /**
     * ✅ ADDED: A native query to find a vote regardless of its retraction status.
     * This is CRITICAL for the service logic when a user changes their vote (e.g., from upvote to downvote).
     * The service needs this to find the old vote record to update it, as the default finders
     * would ignore it due to the @Where(clause = "is_retracted = false") filter.
     */
    @Query(value = "SELECT * FROM votes v WHERE v.user_id = :userId AND v.post_id = :postId LIMIT 1", nativeQuery = true)
    Optional<Vote> findVoteEvenIfRetracted(@Param("userId") Integer userId, @Param("postId") Integer postId);

    /**
     * ✅ ADDED: Counts the number of votes for a post with a specific value.
     * Useful for analytics or for data integrity checks against denormalized counters.
     *
     * @param postId The ID of the post.
     * @param value  The vote value (+1 for upvote, -1 for downvote).
     * @return The total count of votes.
     */
    long countByPost_IdAndValue(Integer postId, short value);

    /**
     * ✅ ADDED: Gets a paginated list of users who voted on a post.
     * Can be used to show "Users who upvoted this".
     *
     * @param postId The ID of the post.
     * @param value The vote value to filter by.
     * @param pageable Pagination information.
     * @return A paginated list of votes.
     */
    Page<Vote> findByPost_IdAndValue(Integer postId, short value, Pageable pageable);
}