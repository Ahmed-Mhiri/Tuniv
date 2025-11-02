package com.tuniv.backend.community.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.qa.model.TopicType;

@Repository
public interface CommunityRepository extends JpaRepository<Community, Integer>, JpaSpecificationExecutor<Community> {

    // ✅ This query is performant as it uses denormalized counters.
    Page<Community> findByNameContainingIgnoreCase(String search, Pageable pageable);
    
    // ✅ ADDED: A simple, direct lookup by name.
    Optional<Community> findByNameIgnoreCase(String name);

    // ✅ These queries correctly use denormalized counts for efficient sorting.
    List<Community> findByOrderByMemberCountDesc(Pageable pageable);
    List<Community> findByOrderByTopicCountDesc(Pageable pageable);

    /**
     * ✨ IMPROVED: The `members` collection was removed from the Community entity.
     * This query now correctly joins through the `CommunityMembership` entity to find
     * all communities a specific user has joined.
     */
    @Query("SELECT c FROM Community c JOIN CommunityMembership cm ON c.communityId = cm.community.communityId WHERE cm.user.userId = :userId")
    List<Community> findCommunitiesByUserId(@Param("userId") Integer userId);

    // ✅ This is a standard and correct query.
    List<Community> findByUniversityIsNull();

    // ✅ These count queries are correct and should be used by services to get stats.
    @Query("SELECT COUNT(t) FROM Topic t WHERE t.community.communityId = :communityId")
    long countTopicsByCommunityId(@Param("communityId") Integer communityId);

    @Query("SELECT COUNT(t) FROM Topic t WHERE t.community.communityId = :communityId AND t.topicType = :topicType")
    long countTopicsByCommunityIdAndType(@Param("communityId") Integer communityId, @Param("topicType") TopicType topicType);

    @Query("SELECT COUNT(t) FROM Topic t WHERE t.community.communityId = :communityId AND t.isSolved = true")
    long countSolvedTopicsByCommunityId(@Param("communityId") Integer communityId);

    /**
     * ❌ DANGEROUS - REWRITTEN: The original method tried to fetch a community with all its topics,
     * which is an anti-pattern that causes severe performance issues.
     * ✨ This method now simply finds the community. Your service layer should then call the
     * `TopicRepository` to get a PAGINATED list of topics for this community.
     */
    @Override
    Optional<Community> findById(Integer communityId);

    // ✅ Correctly uses the denormalized `topicCount`.
    List<Community> findByUniversity_UniversityIdOrderByTopicCountDesc(@Param("universityId") Integer universityId);

    // ✅ This is a good, performant query using a subselect.
    @Query("SELECT c FROM Community c WHERE c.communityId IN " +
           "(SELECT t.community.communityId FROM Topic t WHERE t.createdAt >= :sinceDate) " +
           "ORDER BY c.topicCount DESC")
    List<Community> findActiveCommunitiesSince(@Param("sinceDate") Instant sinceDate, Pageable pageable);

    // ✅ This is another performant query using denormalized `memberCount`.
    List<Community> findByMemberCountGreaterThanEqualOrderByMemberCountDesc(@Param("minMembers") Integer minMembers, Pageable pageable);

    /**
     * 💡 NOTE: The logic from the original `getCommunityTopicStats` DTO query has been removed.
     * That query was invalid because it joined `c.topics`.
     * The CORRECT PATTERN is to fetch the Community object from this repository, and then use the
     * dedicated count methods above (`countTopicsBy...`, `countSolvedTopicsBy...`) to
     * assemble your DTO in the service layer. This is more modular and performant.
     */

    // ✅ This is a good, performant query using a subselect with HAVING.
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

    // === Denormalized Counter Management ===
    // ✅ ADDED: These methods are ESSENTIAL for keeping the Community entity's stats in sync.

    @Modifying
    @Query("UPDATE Community c SET c.memberCount = c.memberCount + 1 WHERE c.communityId = :communityId")
    void incrementMemberCount(@Param("communityId") Integer communityId);

    @Modifying
    @Query("UPDATE Community c SET c.memberCount = c.memberCount - 1 WHERE c.communityId = :communityId AND c.memberCount > 0")
    void decrementMemberCount(@Param("communityId") Integer communityId);

    @Modifying
    @Query("UPDATE Community c SET c.topicCount = c.topicCount + 1 WHERE c.communityId = :communityId")
    void incrementTopicCount(@Param("communityId") Integer communityId);

    @Modifying
    @Query("UPDATE Community c SET c.topicCount = c.topicCount - 1 WHERE c.communityId = :communityId AND c.topicCount > 0")
    void decrementTopicCount(@Param("communityId") Integer communityId);
}