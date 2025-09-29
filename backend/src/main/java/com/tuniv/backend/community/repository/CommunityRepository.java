package com.tuniv.backend.community.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.community.dto.CommunityTopicStatsDto;
import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.qa.model.TopicType;

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

    // ✅ UPDATED: Count all TOPICS in a community (replaces countQuestionsByCommunityId)
    @Query("SELECT COUNT(t) FROM Topic t WHERE t.community.communityId = :communityId")
    long countTopicsByCommunityId(@Param("communityId") Integer communityId);

    // ✅ NEW: Count topics by type in a community
    @Query("SELECT COUNT(t) FROM Topic t WHERE t.community.communityId = :communityId AND t.topicType = :topicType")
    long countTopicsByCommunityIdAndType(
        @Param("communityId") Integer communityId,
        @Param("topicType") TopicType topicType
    );

    // ✅ NEW: Count solved topics in a community
    @Query("SELECT COUNT(t) FROM Topic t WHERE t.community.communityId = :communityId AND t.isSolved = true")
    long countSolvedTopicsByCommunityId(@Param("communityId") Integer communityId);

    // ✅ NEW: Find communities with topics for statistics
    @Query("SELECT c FROM Community c LEFT JOIN FETCH c.topics WHERE c.communityId = :communityId")
    Optional<Community> findWithTopicsById(@Param("communityId") Integer communityId);

    // ✅ NEW: Find top communities by topic count
    @Query("SELECT c FROM Community c ORDER BY c.topicCount DESC, c.memberCount DESC")
    List<Community> findTopCommunitiesByTopicCount(Pageable pageable);

    // ✅ NEW: Find communities by university with topic counts
    @Query("SELECT c FROM Community c WHERE c.university.universityId = :universityId ORDER BY c.topicCount DESC")
    List<Community> findByUniversityIdOrderByTopicCountDesc(@Param("universityId") Integer universityId);

    // ✅ NEW: Find active communities (with recent topics)
    @Query("SELECT c FROM Community c WHERE c.communityId IN " +
           "(SELECT t.community.communityId FROM Topic t WHERE t.createdAt >= :sinceDate) " +
           "ORDER BY c.topicCount DESC")
    List<Community> findActiveCommunitiesSince(@Param("sinceDate") Instant sinceDate, Pageable pageable);

    // ✅ NEW: Check if community exists and is accessible
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Community c WHERE c.communityId = :communityId")
    boolean existsAndAccessible(@Param("communityId") Integer communityId);

    // ✅ NEW: Find communities with minimum member count
    @Query("SELECT c FROM Community c WHERE c.memberCount >= :minMembers ORDER BY c.memberCount DESC")
    List<Community> findPopularCommunities(@Param("minMembers") Integer minMembers, Pageable pageable);

    // ✅ NEW: Search communities with topic count filtering
    @Query("SELECT c FROM Community c WHERE " +
           "(:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:minTopics IS NULL OR c.topicCount >= :minTopics) AND " +
           "(:minMembers IS NULL OR c.memberCount >= :minMembers)")
    Page<Community> findAllWithAdvancedFilters(
        @Param("search") String search,
        @Param("minTopics") Integer minTopics,
        @Param("minMembers") Integer minMembers,
        Pageable pageable
    );

    // ✅ NEW: Get community topic statistics
    @Query("SELECT new com.tuniv.backend.community.dto.CommunityTopicStatsDto(" +
           "c.communityId, c.name, " +
           "COUNT(t), " +
           "SUM(CASE WHEN t.isSolved = true THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.topicType = 'QUESTION' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.topicType = 'POST' THEN 1 ELSE 0 END)" +
           ") FROM Community c LEFT JOIN c.topics t " +
           "WHERE c.communityId = :communityId " +
           "GROUP BY c.communityId, c.name")
    Optional<CommunityTopicStatsDto> getCommunityTopicStats(@Param("communityId") Integer communityId);

    // ✅ NEW: Find trending communities (communities with recent activity)
    @Query("SELECT c FROM Community c WHERE c.communityId IN (" +
           "SELECT t.community.communityId FROM Topic t " +
           "WHERE t.createdAt >= :startDate " +
           "GROUP BY t.community.communityId " +
           "HAVING COUNT(t) >= :minRecentTopics" +
           ") ORDER BY c.topicCount DESC")
    List<Community> findTrendingCommunities(
        @Param("startDate") Instant startDate,
        @Param("minRecentTopics") Long minRecentTopics,
        Pageable pageable
    );
}