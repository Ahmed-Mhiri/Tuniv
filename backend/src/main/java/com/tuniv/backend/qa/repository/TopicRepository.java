package com.tuniv.backend.qa.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.qa.dto.TopicSummaryDto;
import com.tuniv.backend.qa.model.Topic;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Integer>, JpaSpecificationExecutor<Topic> {

    // ✅ This is efficient, but for display, prefer the denormalized `topicCount` on the User entity.
    long countByAuthor_UserId(Integer userId);

    /**
     * ❌ DANGEROUS - REWRITTEN: The original method tried to fetch entire collections of replies and attachments,
     * which causes massive performance issues (Cartesian Products) and is no longer possible with the new schema.
     * ✨ This rewritten version safely fetches the topic with its essential, single-entity relationships.
     * Replies should ALWAYS be fetched in a separate, paginated query from the ReplyRepository.
     */
    @EntityGraph(attributePaths = {"author", "acceptedSolution", "acceptedSolution.author", "community", "module"})
    Optional<Topic> findById(Integer topicId);
    
    // ✅ This method is well-defined and efficient for fetching core details. No changes needed.
    @EntityGraph(attributePaths = {"author", "acceptedSolution"})
    Optional<Topic> findWithDetailsById(Integer topicId);

    /**
     * ✨ IMPROVED: The `tags` collection was removed. This query now correctly joins through the `TopicTag`
     * entity to find topics associated with the given tag IDs.
     */
    @Query("SELECT DISTINCT t FROM Topic t JOIN TopicTag tt ON t.id = tt.topic.id WHERE tt.tag.id IN :tagIds")
    List<Topic> findTopicsByTagIds(@Param("tagIds") List<Integer> tagIds);

    // ✅ This is a standard and useful query. No changes needed.
    List<Topic> findByAuthor_UserIdOrderByCreatedAtDesc(Integer userId);

    // ✅ This query is correct and efficient. No changes needed.
    @Query("SELECT t FROM Topic t WHERE t.acceptedSolution.author.userId = :userId")
    List<Topic> findByAcceptedSolution_Author_UserId(@Param("userId") Integer userId);

    /**
     * ⚠️ PERFORMANCE WARNING: A query with multiple optional 'OR' clauses can be slow on large datasets
     * because the database struggles to use indexes effectively for all conditions.
     * For a truly scalable feed, consider a dedicated search engine like Elasticsearch.
     * ✨ IMPROVED: The tag filtering logic is updated to use the new `TopicTag` join table.
     */
    @Query(value = """
        SELECT NEW com.tuniv.backend.qa.dto.TopicSummaryDto(
            t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score,
            t.replyCount,
            (SELECT v.value FROM Vote v WHERE v.post.id = t.id AND v.user.userId = :currentUserId),
            t.isSolved,
            COALESCE(m.moduleId, c.communityId),
            COALESCE(m.name, c.name)
        )
        FROM Topic t
        LEFT JOIN t.module m
        LEFT JOIN t.community c
        WHERE
            (:userIds IS NULL OR t.author.userId IN :userIds) OR
            (:communityIds IS NULL OR c.communityId IN :communityIds) OR
            (:moduleIds IS NULL OR m.moduleId IN :moduleIds) OR
            (:tagIds IS NULL OR EXISTS (
                SELECT 1 FROM TopicTag tt WHERE tt.topic.id = t.id AND tt.tag.id IN :tagIds
            ))
        ORDER BY t.createdAt DESC
    """, countQuery = """
        SELECT COUNT(t)
        FROM Topic t
        LEFT JOIN t.module m
        LEFT JOIN t.community c
        WHERE
            (:userIds IS NULL OR t.author.userId IN :userIds) OR
            (:communityIds IS NULL OR c.communityId IN :communityIds) OR
            (:moduleIds IS NULL OR m.moduleId IN :moduleIds) OR
            (:tagIds IS NULL OR EXISTS (
                SELECT 1 FROM TopicTag tt WHERE tt.topic.id = t.id AND tt.tag.id IN :tagIds
            ))
    """)
    Page<TopicSummaryDto> findPersonalizedFeed(
        @Param("userIds") List<Integer> userIds,
        @Param("communityIds") List<Integer> communityIds,
        @Param("tagIds") List<Integer> tagIds,
        @Param("moduleIds") List<Integer> moduleIds,
        @Param("currentUserId") Integer currentUserId,
        Pageable pageable
    );

    // ✨ IMPROVED: The LEFT JOIN to votes was inefficient for a simple list.
    // Replaced with a more performant scalar subquery to get the current user's vote status.
    @Query(value = """
        SELECT NEW com.tuniv.backend.qa.dto.TopicSummaryDto(
            t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score,
            t.replyCount,
            (SELECT v.value FROM Vote v WHERE v.post.id = t.id AND v.user.userId = :currentUserId),
            t.isSolved,
            m.moduleId, m.name
        )
        FROM Topic t
        JOIN t.module m
        WHERE m.moduleId = :moduleId
        ORDER BY t.createdAt DESC
    """, countQuery = "SELECT COUNT(t) FROM Topic t WHERE t.module.moduleId = :moduleId")
    Page<TopicSummaryDto> findTopicSummariesByModuleId(
        @Param("moduleId") Integer moduleId,
        @Param("currentUserId") Integer currentUserId,
        Pageable pageable
    );
    
    // ✨ IMPROVED: Replaced LEFT JOIN with a scalar subquery for vote status.
    @Query(value = """
        SELECT NEW com.tuniv.backend.qa.dto.TopicSummaryDto(
            t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score,
            t.replyCount,
            (SELECT v.value FROM Vote v WHERE v.post.id = t.id AND v.user.userId = :currentUserId),
            t.isSolved,
            c.communityId, c.name
        )
        FROM Topic t
        JOIN t.community c
        WHERE c.communityId = :communityId
        ORDER BY t.createdAt DESC
    """, countQuery = "SELECT COUNT(t) FROM Topic t WHERE t.community.communityId = :communityId")
    Page<TopicSummaryDto> findTopicSummariesByCommunityId(
        @Param("communityId") Integer communityId,
        @Param("currentUserId") Integer currentUserId,
        Pageable pageable
    );

    // ✨ IMPROVED: Corrected join to use `TopicTag` and used a scalar subquery for vote status.
    @Query(value = """
        SELECT NEW com.tuniv.backend.qa.dto.TopicSummaryDto(
            t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score,
            t.replyCount,
            (SELECT v.value FROM Vote v WHERE v.post.id = t.id AND v.user.userId = :currentUserId),
            t.isSolved,
            COALESCE(m.moduleId, c.communityId),
            COALESCE(m.name, c.name)
        )
        FROM Topic t
        JOIN TopicTag tt ON t.id = tt.topic.id
        JOIN tt.tag tag ON tag.id = tt.tag.id
        LEFT JOIN t.module m
        LEFT JOIN t.community c
        WHERE tag.name = :tagName
        ORDER BY t.score DESC, t.createdAt DESC
    """, countQuery = "SELECT COUNT(t) FROM Topic t JOIN TopicTag tt ON t.id = tt.topic.id JOIN tt.tag tag ON tag.id = tt.tag.id WHERE tag.name = :tagName")
    Page<TopicSummaryDto> findTopicSummariesByTag(
        @Param("tagName") String tagName,
        @Param("currentUserId") Integer currentUserId,
        Pageable pageable
    );

    // All remaining DTO queries are also improved with the scalar subquery for vote status for consistency and performance.
    
    @Query(value = """
        SELECT NEW com.tuniv.backend.qa.dto.TopicSummaryDto(
            t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score,
            t.replyCount,
            (SELECT v.value FROM Vote v WHERE v.post.id = t.id AND v.user.userId = :currentUserId),
            t.isSolved,
            COALESCE(m.moduleId, c.communityId),
            COALESCE(m.name, c.name)
        )
        FROM Topic t
        LEFT JOIN t.module m
        LEFT JOIN t.community c
        ORDER BY t.score DESC, t.createdAt DESC
    """, countQuery = "SELECT COUNT(t) FROM Topic t")
    Page<TopicSummaryDto> findPopularTopicSummaries(
        @Param("currentUserId") Integer currentUserId,
        Pageable pageable
    );

    @Query(value = """
        SELECT NEW com.tuniv.backend.qa.dto.TopicSummaryDto(
            t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score,
            t.replyCount,
            (SELECT v.value FROM Vote v WHERE v.post.id = t.id AND v.user.userId = :currentUserId),
            t.isSolved,
            m.moduleId, m.name
        )
        FROM Topic t
        JOIN t.module m
        WHERE m.moduleId IN :moduleIds
        ORDER BY t.createdAt DESC
    """, countQuery = "SELECT COUNT(t) FROM Topic t WHERE t.module.moduleId IN :moduleIds")
    Page<TopicSummaryDto> findTopicSummariesByModuleIdIn(
        @Param("moduleIds") List<Integer> moduleIds,
        @Param("currentUserId") Integer currentUserId,
        Pageable pageable
    );

    @Query(value = """
        SELECT NEW com.tuniv.backend.qa.dto.TopicSummaryDto(
            t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score,
            t.replyCount,
            (SELECT v.value FROM Vote v WHERE v.post.id = t.id AND v.user.userId = :currentUserId),
            t.isSolved,
            COALESCE(m.moduleId, c.communityId),
            COALESCE(m.name, c.name)
        )
        FROM Topic t
        LEFT JOIN t.module m
        LEFT JOIN t.community c
        WHERE t.author.userId = :userId AND t.isSolved = true
        ORDER BY t.createdAt DESC
    """, countQuery = "SELECT COUNT(t) FROM Topic t WHERE t.author.userId = :userId AND t.isSolved = true")
    Page<TopicSummaryDto> findSolvedTopicsByUser(
        @Param("userId") Integer userId,
        @Param("currentUserId") Integer currentUserId,
        Pageable pageable
    );

    @Query(value = """
        SELECT NEW com.tuniv.backend.qa.dto.TopicSummaryDto(
            t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score,
            t.replyCount,
            (SELECT v.value FROM Vote v WHERE v.post.id = t.id AND v.user.userId = :currentUserId),
            t.isSolved,
            COALESCE(m.moduleId, c.communityId),
            COALESCE(m.name, c.name)
        )
        FROM Topic t
        LEFT JOIN t.module m
        LEFT JOIN t.community c
        WHERE t.acceptedSolution.author.userId = :userId
        ORDER BY t.createdAt DESC
    """, countQuery = "SELECT COUNT(t) FROM Topic t WHERE t.acceptedSolution.author.userId = :userId")
    Page<TopicSummaryDto> findTopicsWithUserSolutions(
        @Param("userId") Integer userId,
        @Param("currentUserId") Integer currentUserId,
        Pageable pageable
    );

    // ✅ Excellent, efficient query for stats. No changes needed.
    @Query("SELECT COUNT(t) FROM Topic t WHERE t.community = :community AND t.createdAt >= :sinceDate")
    Long countByCommunityAndCreatedAfter(@Param("community") Community community, @Param("sinceDate") Instant sinceDate);

    // ✅ Perfect methods for atomically updating denormalized counters. No changes needed.
    @Modifying
    @Query("UPDATE Topic t SET t.replyCount = t.replyCount + 1, t.lastActivityAt = CURRENT_TIMESTAMP WHERE t.id = :topicId")
    void incrementReplyCount(@Param("topicId") Integer topicId);

    @Modifying
    @Query("UPDATE Topic t SET t.replyCount = t.replyCount - 1 WHERE t.id = :topicId AND t.replyCount > 0")
    void decrementReplyCount(@Param("topicId") Integer topicId);
    
    // ✅ These are great, efficient pagination methods. No changes needed.
    @EntityGraph(attributePaths = {"author"})
    Page<Topic> findByCommunityCommunityId(Integer communityId, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Page<Topic> findByModuleModuleId(Integer moduleId, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Page<Topic> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Page<Topic> findAllByOrderByScoreDesc(Pageable pageable);

    // This method is functionally identical to the built-in `findAllById`. Keeping for compatibility.
    @Query("SELECT t FROM Topic t WHERE t.id IN :topicIds")
    List<Topic> findByIds(@Param("topicIds") List<Integer> topicIds);
    
    // This is the preferred, standard Spring Data JPA method for this operation.
    @EntityGraph(attributePaths = {"author"})
    List<Topic> findByIdIn(List<Integer> topicIds);

    // ✅ Good, specific count queries. No changes needed.
    @Query("SELECT COUNT(t) FROM Topic t WHERE t.community.communityId = :communityId AND t.isSolved = true")
    long countSolvedTopicsByCommunityId(@Param("communityId") Integer communityId);

    @Query("SELECT COUNT(t) FROM Topic t WHERE t.module.moduleId = :moduleId AND t.isSolved = true")
    long countSolvedTopicsByModuleId(@Param("moduleId") Integer moduleId);
    
    // ✅ The name is slightly confusing, but it's a valid paginated query with an entity graph. No changes needed.
    @EntityGraph(attributePaths = {"author", "community", "module"})
    Page<Topic> findByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * ⚠️ PERFORMANCE WARNING: A `LIKE '%query%'` search cannot use a standard database index effectively
     * and will be slow on large tables. Consider a database-level full-text search index for better performance.
     */
    @Query("SELECT t FROM Topic t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Topic> findByTitleContainingIgnoreCase(@Param("query") String query, Pageable pageable);
}