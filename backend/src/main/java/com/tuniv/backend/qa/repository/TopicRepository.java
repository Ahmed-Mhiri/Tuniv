package com.tuniv.backend.qa.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.qa.dto.TopicSummaryDto;
import com.tuniv.backend.qa.model.Topic;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Integer> {

    long countByAuthor_UserId(Integer userId);

    @EntityGraph(attributePaths = {"author", "attachments", "replies", "replies.author", "replies.childReplies"})
    Optional<Topic> findFullTreeById(Integer topicId);

    @EntityGraph(attributePaths = {"author", "acceptedSolution"})
    Optional<Topic> findWithDetailsById(Integer topicId);

    @Query("SELECT t FROM Topic t JOIN FETCH t.tags WHERE t.id IN :topicIds")
    List<Topic> findWithTagsByIdIn(@Param("topicIds") List<Integer> topicIds);

    List<Topic> findByAuthor_UserIdOrderByCreatedAtDesc(Integer userId);

    // ✅ NEW: Find topics where user's reply is the accepted solution
    @Query("SELECT t FROM Topic t WHERE t.acceptedSolution.author.userId = :userId")
    List<Topic> findByAcceptedSolution_Author_UserId(@Param("userId") Integer userId);

    // ✅ PERFORMANCE: Personalized feed query - FIXED FIELD REFERENCES
    @Query(value = """
        SELECT NEW com.tuniv.backend.dto.TopicSummaryDto(
            t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score,
            (SELECT COUNT(r.id) FROM Reply r WHERE r.topic.id = t.id),
            CASE
                WHEN v.value = 1 THEN 'UPVOTE'
                WHEN v.value = -1 THEN 'DOWNVOTE'
                ELSE NULL
            END,
            t.isSolved,
            COALESCE(m.moduleId, c.communityId),
            COALESCE(m.name, c.name)
        )
        FROM Topic t
        LEFT JOIN t.module m 
        LEFT JOIN m.university u
        LEFT JOIN t.community c
        LEFT JOIN t.votes v ON v.user.userId = :currentUserId
        WHERE 
            (:userIds IS NULL OR t.author.userId IN :userIds) OR
            (:communityIds IS NULL OR c.communityId IN :communityIds) OR
            (:moduleIds IS NULL OR m.moduleId IN :moduleIds) OR
            (:tagIds IS NULL OR EXISTS (SELECT 1 FROM t.tags tag WHERE tag.id IN :tagIds))
        GROUP BY t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score, 
                 m.moduleId, m.name, c.communityId, c.name, v.value, t.isSolved
        ORDER BY t.createdAt DESC
    """, countQuery = """
        SELECT COUNT(DISTINCT t) 
        FROM Topic t
        LEFT JOIN t.module m 
        LEFT JOIN t.community c
        WHERE 
            (:userIds IS NULL OR t.author.userId IN :userIds) OR
            (:communityIds IS NULL OR c.communityId IN :communityIds) OR
            (:moduleIds IS NULL OR m.moduleId IN :moduleIds) OR
            (:tagIds IS NULL OR EXISTS (SELECT 1 FROM t.tags tag WHERE tag.id IN :tagIds))
    """)
    Page<TopicSummaryDto> findPersonalizedFeed(
        @Param("userIds") List<Integer> userIds,
        @Param("communityIds") List<Integer> communityIds,
        @Param("tagIds") List<Integer> tagIds,
        @Param("moduleIds") List<Integer> moduleIds,
        @Param("currentUserId") Integer currentUserId,
        Pageable pageable
    );

    @Query(value = """
        SELECT NEW com.tuniv.backend.dto.TopicSummaryDto(
            t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score,
            (SELECT COUNT(r.id) FROM Reply r WHERE r.topic.id = t.id),
            CASE
                WHEN v.value = 1 THEN 'UPVOTE'
                WHEN v.value = -1 THEN 'DOWNVOTE'
                ELSE NULL
            END,
            t.isSolved,
            m.moduleId, m.name
        )
        FROM Topic t
        JOIN t.module m 
        JOIN m.university u
        LEFT JOIN t.votes v ON v.user.userId = :currentUserId
        WHERE m.moduleId = :moduleId
        GROUP BY t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score, 
                 m.moduleId, m.name, v.value, t.isSolved
        ORDER BY t.createdAt DESC
    """, countQuery = "SELECT COUNT(t) FROM Topic t WHERE t.module.moduleId = :moduleId")
    Page<TopicSummaryDto> findTopicSummariesByModuleId(
        @Param("moduleId") Integer moduleId,
        @Param("currentUserId") Integer currentUserId,
        Pageable pageable
    );

    @Query(value = """
        SELECT NEW com.tuniv.backend.dto.TopicSummaryDto(
            t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score,
            (SELECT COUNT(r.id) FROM Reply r WHERE r.topic.id = t.id),
            CASE
                WHEN v.value = 1 THEN 'UPVOTE'
                WHEN v.value = -1 THEN 'DOWNVOTE'
                ELSE NULL
            END,
            t.isSolved,
            c.communityId, c.name
        )
        FROM Topic t
        JOIN t.community c
        LEFT JOIN t.votes v ON v.user.userId = :currentUserId
        WHERE c.communityId = :communityId
        GROUP BY t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score, 
                 c.communityId, c.name, v.value, t.isSolved
        ORDER BY t.createdAt DESC
    """, countQuery = "SELECT COUNT(t) FROM Topic t WHERE t.community.communityId = :communityId")
    Page<TopicSummaryDto> findTopicSummariesByCommunityId(
        @Param("communityId") Integer communityId,
        @Param("currentUserId") Integer currentUserId,
        Pageable pageable
    );

    @Query(value = """
        SELECT NEW com.tuniv.backend.dto.TopicSummaryDto(
            t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score,
            (SELECT COUNT(r.id) FROM Reply r WHERE r.topic.id = t.id),
            CASE
                WHEN v.value = 1 THEN 'UPVOTE'
                WHEN v.value = -1 THEN 'DOWNVOTE'
                ELSE NULL
            END,
            t.isSolved,
            COALESCE(m.moduleId, c.communityId),
            COALESCE(m.name, c.name)
        )
        FROM Topic t
        LEFT JOIN t.module m 
        LEFT JOIN m.university u
        LEFT JOIN t.community c
        LEFT JOIN t.votes v ON v.user.userId = :currentUserId
        JOIN t.tags tag
        WHERE tag.name = :tagName
        GROUP BY t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score, 
                 m.moduleId, m.name, c.communityId, c.name, v.value, t.isSolved
        ORDER BY t.score DESC, t.createdAt DESC
    """, countQuery = "SELECT COUNT(t) FROM Topic t JOIN t.tags tag WHERE tag.name = :tagName")
    Page<TopicSummaryDto> findTopicSummariesByTag(
        @Param("tagName") String tagName,
        @Param("currentUserId") Integer currentUserId,
        Pageable pageable
    );

    @Query(value = """
        SELECT NEW com.tuniv.backend.dto.TopicSummaryDto(
            t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score,
            (SELECT COUNT(r.id) FROM Reply r WHERE r.topic.id = t.id),
            CASE
                WHEN v.value = 1 THEN 'UPVOTE'
                WHEN v.value = -1 THEN 'DOWNVOTE'
                ELSE NULL
            END,
            t.isSolved,
            COALESCE(m.moduleId, c.communityId),
            COALESCE(m.name, c.name)
        )
        FROM Topic t
        LEFT JOIN t.module m 
        LEFT JOIN m.university u
        LEFT JOIN t.community c
        LEFT JOIN t.votes v ON v.user.userId = :currentUserId
        GROUP BY t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score, 
                 m.moduleId, m.name, c.communityId, c.name, v.value, t.isSolved
        ORDER BY t.score DESC, t.createdAt DESC
    """, countQuery = "SELECT COUNT(t) FROM Topic t")
    Page<TopicSummaryDto> findPopularTopicSummaries(
        @Param("currentUserId") Integer currentUserId,
        Pageable pageable
    );

    @Query(value = """
        SELECT NEW com.tuniv.backend.dto.TopicSummaryDto(
            t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score,
            (SELECT COUNT(r.id) FROM Reply r WHERE r.topic.id = t.id),
            CASE
                WHEN v.value = 1 THEN 'UPVOTE'
                WHEN v.value = -1 THEN 'DOWNVOTE'
                ELSE NULL
            END,
            t.isSolved,
            m.moduleId, m.name
        )
        FROM Topic t
        JOIN t.module m 
        JOIN m.university u
        LEFT JOIN t.votes v ON v.user.userId = :currentUserId
        WHERE m.moduleId IN :moduleIds
        GROUP BY t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score, 
                 m.moduleId, m.name, v.value, t.isSolved
        ORDER BY t.createdAt DESC
    """, countQuery = "SELECT COUNT(t) FROM Topic t WHERE t.module.moduleId IN :moduleIds")
    Page<TopicSummaryDto> findTopicSummariesByModuleIdIn(
        @Param("moduleIds") List<Integer> moduleIds,
        @Param("currentUserId") Integer currentUserId,
        Pageable pageable
    );

    // ✅ NEW: Find solved topics by a user
@Query(value = """
    SELECT NEW com.tuniv.backend.dto.TopicSummaryDto(
        t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score,
        (SELECT COUNT(r.id) FROM Reply r WHERE r.topic.id = t.id),
        CASE
            WHEN v.value = 1 THEN 'UPVOTE'
            WHEN v.value = -1 THEN 'DOWNVOTE'
            ELSE NULL
        END,
        t.isSolved,
        COALESCE(m.moduleId, c.communityId),
        COALESCE(m.name, c.name)
    )
    FROM Topic t
    LEFT JOIN t.module m 
    LEFT JOIN t.community c
    LEFT JOIN t.votes v ON v.user.userId = :currentUserId
    WHERE t.author.userId = :userId AND t.isSolved = true
    GROUP BY t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score, 
             m.moduleId, m.name, c.communityId, c.name, v.value, t.isSolved
    ORDER BY t.createdAt DESC
""", countQuery = "SELECT COUNT(t) FROM Topic t WHERE t.author.userId = :userId AND t.isSolved = true")
Page<TopicSummaryDto> findSolvedTopicsByUser(
    @Param("userId") Integer userId,
    @Param("currentUserId") Integer currentUserId,
    Pageable pageable
);

// ✅ NEW: Find topics where user's reply is the accepted solution
@Query(value = """
    SELECT NEW com.tuniv.backend.dto.TopicSummaryDto(
        t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score,
        (SELECT COUNT(r.id) FROM Reply r WHERE r.topic.id = t.id),
        CASE
            WHEN v.value = 1 THEN 'UPVOTE'
            WHEN v.value = -1 THEN 'DOWNVOTE'
            ELSE NULL
        END,
        t.isSolved,
        COALESCE(m.moduleId, c.communityId),
        COALESCE(m.name, c.name)
    )
    FROM Topic t
    LEFT JOIN t.module m 
    LEFT JOIN t.community c
    LEFT JOIN t.votes v ON v.user.userId = :currentUserId
    WHERE t.acceptedSolution.author.userId = :userId
    GROUP BY t.id, t.title, t.topicType, t.author.userId, t.author.username, t.createdAt, t.score, 
             m.moduleId, m.name, c.communityId, c.name, v.value, t.isSolved
    ORDER BY t.createdAt DESC
""", countQuery = "SELECT COUNT(t) FROM Topic t WHERE t.acceptedSolution.author.userId = :userId")
Page<TopicSummaryDto> findTopicsWithUserSolutions(
    @Param("userId") Integer userId,
    @Param("currentUserId") Integer currentUserId,
    Pageable pageable
);

// ✅ NEW: Count topics by community and creation date
@Query("SELECT COUNT(t) FROM Topic t WHERE t.community = :community AND t.createdAt >= :sinceDate")
Long countByCommunityAndCreatedAfter(
    @Param("community") Community community,
    @Param("sinceDate") Instant sinceDate
);
}