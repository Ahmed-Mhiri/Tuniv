package com.tuniv.backend.qa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.dto.QuestionSummaryDto;
import com.tuniv.backend.qa.model.Question;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {

    long countByAuthor_UserId(Integer userId);

    @EntityGraph(value = "question-with-full-tree", type = EntityGraph.EntityGraphType.FETCH)
    Optional<Question> findFullTreeById(Integer questionId);

    @Query("SELECT q FROM Question q JOIN FETCH q.tags WHERE q.id IN :questionIds")
    List<Question> findWithTagsByIdIn(@Param("questionIds") List<Integer> questionIds);

    List<Question> findByAuthor_UserIdOrderByCreatedAtDesc(Integer userId);

    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.attachments WHERE q.id IN :questionIds")
    List<Question> findWithAttachmentsByIdIn(@Param("questionIds") List<Integer> questionIds);

    // NEW: Personalized feed query based on user's follows
    @Query(value = """
        SELECT NEW com.tuniv.backend.qa.dto.QuestionSummaryDto(
            q.id, q.title, q.author.id, q.author.username, q.createdAt, q.score, q.answerCount,
            CASE
                WHEN v.value = 1 THEN 'UPVOTE'
                WHEN v.value = -1 THEN 'DOWNVOTE'
                ELSE NULL
            END,
            (SELECT COUNT(a.id) > 0 FROM Answer a WHERE a.question.id = q.id AND a.isSolution = true),
            u.id, u.name
        )
        FROM Question q
        JOIN q.author
        JOIN q.module m JOIN m.university u
        LEFT JOIN q.votes v ON v.user.id = :currentUserId
        WHERE 
            (:userIds IS NULL OR q.author.id IN :userIds) OR
            (:communityIds IS NULL OR q.community.id IN :communityIds) OR
            (:moduleIds IS NULL OR m.moduleId IN :moduleIds) OR
            (:tagIds IS NULL OR EXISTS (SELECT 1 FROM q.tags t WHERE t.id IN :tagIds))
        GROUP BY q.id, q.title, q.author.id, q.author.username, q.createdAt, q.score, q.answerCount, u.id, u.name, v.value
        ORDER BY q.createdAt DESC
    """, countQuery = """
        SELECT COUNT(DISTINCT q) 
        FROM Question q
        JOIN q.module m
        WHERE 
            (:userIds IS NULL OR q.author.id IN :userIds) OR
            (:communityIds IS NULL OR q.community.id IN :communityIds) OR
            (:moduleIds IS NULL OR m.moduleId IN :moduleIds) OR
            (:tagIds IS NULL OR EXISTS (SELECT 1 FROM q.tags t WHERE t.id IN :tagIds))
    """)
    Page<QuestionSummaryDto> findPersonalizedFeed(
        @Param("userIds") List<Integer> userIds,
        @Param("communityIds") List<Integer> communityIds,
        @Param("tagIds") List<Integer> tagIds,
        @Param("moduleIds") List<Integer> moduleIds,
        @Param("currentUserId") Integer currentUserId,
        Pageable pageable
    );

    @Query(value = """
        SELECT NEW com.tuniv.backend.qa.dto.QuestionSummaryDto(
            q.id, q.title, q.author.id, q.author.username, q.createdAt, q.score, q.answerCount,
            CASE
                WHEN v.value = 1 THEN 'UPVOTE'
                WHEN v.value = -1 THEN 'DOWNVOTE'
                ELSE NULL
            END,
            (SELECT COUNT(a.id) > 0 FROM Answer a WHERE a.question.id = q.id AND a.isSolution = true),
            u.id, u.name
        )
        FROM Question q
        JOIN q.author
        JOIN q.module m JOIN m.university u
        LEFT JOIN q.votes v ON v.user.id = :currentUserId
        WHERE m.moduleId = :moduleId
        GROUP BY q.id, q.title, q.author.id, q.author.username, q.createdAt, q.score, q.answerCount, u.id, u.name, v.value
    """, countQuery = "SELECT COUNT(q) FROM Question q WHERE q.module.moduleId = :moduleId")
    Page<QuestionSummaryDto> findQuestionSummariesByModuleId(
        @Param("moduleId") Integer moduleId,
        @Param("currentUserId") Integer currentUserId,
        Pageable pageable
    );

    @Query(value = """
        SELECT NEW com.tuniv.backend.qa.dto.QuestionSummaryDto(
            q.id, q.title, q.author.id, q.author.username, q.createdAt, q.score, q.answerCount,
            CASE
                WHEN v.value = 1 THEN 'UPVOTE'
                WHEN v.value = -1 THEN 'DOWNVOTE'
                ELSE NULL
            END,
            (SELECT COUNT(a.id) > 0 FROM Answer a WHERE a.question.id = q.id AND a.isSolution = true),
            u.id, u.name
        )
        FROM Question q
        JOIN q.author
        JOIN q.module m JOIN m.university u
        LEFT JOIN q.votes v ON v.user.id = :currentUserId
        WHERE m.moduleId IN :moduleIds
        GROUP BY q.id, q.title, q.author.id, q.author.username, q.createdAt, q.score, q.answerCount, u.id, u.name, v.value
    """, countQuery = "SELECT COUNT(q) FROM Question q WHERE q.module.moduleId IN :moduleIds")
    Page<QuestionSummaryDto> findQuestionSummariesByModuleIdIn(
        @Param("moduleIds") List<Integer> moduleIds,
        @Param("currentUserId") Integer currentUserId,
        Pageable pageable
    );

    @Query(value = """
        SELECT NEW com.tuniv.backend.qa.dto.QuestionSummaryDto(
            q.id, q.title, q.author.id, q.author.username, q.createdAt, q.score, q.answerCount,
            CASE
                WHEN v.value = 1 THEN 'UPVOTE'
                WHEN v.value = -1 THEN 'DOWNVOTE'
                ELSE NULL
            END,
            (SELECT COUNT(a.id) > 0 FROM Answer a WHERE a.question.id = q.id AND a.isSolution = true),
            u.id, u.name
        )
        FROM Question q
        JOIN q.author
        JOIN q.module m JOIN m.university u
        JOIN q.tags t
        LEFT JOIN q.votes v ON v.user.id = :currentUserId
        WHERE t.name = :tagName
        GROUP BY q.id, q.title, q.author.id, q.author.username, q.createdAt, q.score, q.answerCount, u.id, u.name, v.value
    """, countQuery = "SELECT COUNT(q) FROM Question q JOIN q.tags t WHERE t.name = :tagName")
    Page<QuestionSummaryDto> findQuestionSummariesByTag(
        @Param("tagName") String tagName,
        @Param("currentUserId") Integer currentUserId,
        Pageable pageable
    );
    
    @Query(value = """
        SELECT NEW com.tuniv.backend.qa.dto.QuestionSummaryDto(
            q.id, q.title, q.author.id, q.author.username, q.createdAt, q.score, q.answerCount,
            CASE
                WHEN v.value = 1 THEN 'UPVOTE'
                WHEN v.value = -1 THEN 'DOWNVOTE'
                ELSE NULL
            END,
            (SELECT COUNT(a.id) > 0 FROM Answer a WHERE a.question.id = q.id AND a.isSolution = true),
            u.id, u.name
        )
        FROM Question q
        JOIN q.author
        JOIN q.module m JOIN m.university u
        LEFT JOIN q.votes v ON v.user.id = :currentUserId
        GROUP BY q.id, q.title, q.author.id, q.author.username, q.createdAt, q.score, q.answerCount, u.id, u.name, v.value
    """, countQuery = "SELECT COUNT(q) FROM Question q")
    Page<QuestionSummaryDto> findPopularQuestionSummaries(
        @Param("currentUserId") Integer currentUserId,
        Pageable pageable
    );


    @Query(value = """
    SELECT NEW com.tuniv.backend.qa.dto.QuestionSummaryDto(
        q.id, q.title, q.author.id, q.author.username, q.createdAt, q.score, q.answerCount,
        CASE
            WHEN v.value = 1 THEN 'UPVOTE'
            WHEN v.value = -1 THEN 'DOWNVOTE'
            ELSE NULL
        END,
        (SELECT COUNT(a.id) > 0 FROM Answer a WHERE a.question.id = q.id AND a.isSolution = true),
        u.id, u.name
    )
    FROM Question q
    JOIN q.author
    LEFT JOIN q.community c 
    LEFT JOIN q.module m JOIN m.university u
    LEFT JOIN q.votes v ON v.user.id = :currentUserId
    WHERE c.communityId = :communityId
    GROUP BY q.id, q.title, q.author.id, q.author.username, q.createdAt, q.score, q.answerCount, u.id, u.name, v.value
""", countQuery = "SELECT COUNT(q) FROM Question q WHERE q.community.communityId = :communityId")
Page<QuestionSummaryDto> findQuestionSummariesByCommunityId(
    @Param("communityId") Integer communityId,
    @Param("currentUserId") Integer currentUserId,
    Pageable pageable
);
}