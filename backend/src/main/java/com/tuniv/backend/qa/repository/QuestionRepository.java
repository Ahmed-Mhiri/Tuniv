package com.tuniv.backend.qa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // <-- IMPORT ADDED
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository; // <-- IMPORT ADDED
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.dto.QuestionSummaryDto;
import com.tuniv.backend.qa.model.Question;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {

     @EntityGraph(value = "question-with-full-tree", type = EntityGraph.EntityGraphType.FETCH)
    Optional<Question> findFullTreeById(Integer questionId);

    List<Question> findByAuthor_UserIdOrderByCreatedAtDesc(Integer userId);

    @Query("SELECT q FROM Question q LEFT JOIN FETCH q.attachments WHERE q.id = :questionId")
    Optional<Question> findByIdWithDetails(@Param("questionId") Integer questionId);
    
    // --- NEW OPTIMIZED METHODS ---

    @Query("SELECT q FROM Question q JOIN FETCH q.author JOIN FETCH q.module WHERE q.id = :questionId")
    Optional<Question> findWithAuthorAndModuleById(@Param("questionId") Integer questionId);

    @Query(value = "SELECT q FROM Question q JOIN FETCH q.author WHERE q.module.moduleId = :moduleId",
           countQuery = "SELECT COUNT(q) FROM Question q WHERE q.module.moduleId = :moduleId")
    Page<Question> findByModuleIdWithAuthor(@Param("moduleId") Integer moduleId, Pageable pageable);
    
    @Query("SELECT q FROM Question q JOIN FETCH q.author WHERE q.module.moduleId IN :moduleIds")
    Page<Question> findByModule_ModuleIdInWithAuthor(@Param("moduleIds") List<Integer> moduleIds, Pageable pageable);

    @Query(value = "SELECT q FROM Question q JOIN FETCH q.author LEFT JOIN q.votes v GROUP BY q.id, q.author.id ORDER BY SUM(COALESCE(v.value, 0)) DESC, q.createdAt DESC",
           countQuery = "SELECT COUNT(q) FROM Question q")
    Page<Question> findPopularQuestionsWithAuthor(Pageable pageable);

    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.attachments WHERE q.id IN :questionIds")
    List<Question> findWithAttachmentsByIdIn(@Param("questionIds") List<Integer> questionIds);

    // --- DTO PROJECTION QUERIES ---

    @Query("""
        SELECT NEW com.tuniv.backend.qa.dto.QuestionSummaryDto(
            q.id, q.title, q.author.id, q.author.username, q.createdAt,
            q.score, COUNT(a.id), 0
        )
        FROM Question q
        LEFT JOIN q.answers a
        WHERE q.module.moduleId = :moduleId
        GROUP BY q.id, q.author.id, q.author.username
    """)
    Page<QuestionSummaryDto> findQuestionSummariesByModuleId(@Param("moduleId") Integer moduleId, Pageable pageable);

    @Query("""
        SELECT NEW com.tuniv.backend.qa.dto.QuestionSummaryDto(
            q.id, q.title, q.author.id, q.author.username, q.createdAt,
            q.score, COUNT(a.id), 0
        )
        FROM Question q
        LEFT JOIN q.answers a
        WHERE q.module.moduleId IN :moduleIds
        GROUP BY q.id, q.author.id, q.author.username
    """)
    Page<QuestionSummaryDto> findQuestionSummariesByModuleIdIn(@Param("moduleIds") List<Integer> moduleIds, Pageable pageable);
    
    @Query("""
        SELECT NEW com.tuniv.backend.qa.dto.QuestionSummaryDto(
            q.id, q.title, q.author.id, q.author.username, q.createdAt,
            q.score, COUNT(a.id), 0
        )
        FROM Question q
        LEFT JOIN q.answers a
        GROUP BY q.id, q.author.id, q.author.username
        ORDER BY q.score DESC, q.createdAt DESC
    """)
    Page<QuestionSummaryDto> findPopularQuestionSummaries(Pageable pageable);
}
