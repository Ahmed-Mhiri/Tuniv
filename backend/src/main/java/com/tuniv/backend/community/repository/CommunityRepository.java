package com.tuniv.backend.community.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.community.model.Community;

@Repository
public interface CommunityRepository extends JpaRepository<Community, Integer> {

    // Search communities by name (case-insensitive) with optional university filter
    @Query("SELECT c FROM Community c WHERE " +
           "(:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:universityId IS NULL OR c.university.universityId = :universityId)")
    Page<Community> findAllWithFilters(
        @Param("search") String search,
        @Param("universityId") Integer universityId,
        Pageable pageable
    );

    // Get top communities by member count
    @Query("SELECT c FROM Community c ORDER BY c.memberCount DESC")
    List<Community> findTopCommunities(Pageable pageable);

    // Get communities joined by a specific user
    @Query("SELECT c FROM Community c JOIN c.members m WHERE m.user.userId = :userId")
    List<Community> findCommunitiesByUserId(@Param("userId") Integer userId);

    // Get global communities (no university)
    List<Community> findByUniversityIsNull();

    // ✅ CORRECTED: Count only QUESTIONS in a community (not all posts)
    @Query("SELECT COUNT(q) FROM Question q WHERE q.community.communityId = :communityId")
    long countQuestionsByCommunityId(@Param("communityId") Integer communityId);
}