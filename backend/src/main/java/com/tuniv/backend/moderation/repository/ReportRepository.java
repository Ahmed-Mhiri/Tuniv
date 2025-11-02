package com.tuniv.backend.moderation.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.moderation.model.Report;
import com.tuniv.backend.moderation.model.ReportScope;
import com.tuniv.backend.moderation.model.ReportStatus;

@Repository
public interface ReportRepository extends JpaRepository<Report, Integer>, JpaSpecificationExecutor<Report> {

    /**
     * Finds a report by its target, used to prevent duplicate reports.
     */
    Optional<Report> findByTargetTypeAndTargetIdAndStatus(String targetType, Integer targetId, ReportStatus status);

    /**
     * Finds platform-level reports by scope and status.
     */
    Page<Report> findByScopeAndStatus(ReportScope scope, ReportStatus status, Pageable pageable);

    /**
     * Finds university-level reports by scope, university ID, and status.
     */
    Page<Report> findByScopeAndUniversityIdAndStatus(ReportScope scope, Integer universityId, ReportStatus status, Pageable pageable);

    /**
     * Finds community-level reports by scope, community ID, and status.
     */
    Page<Report> findByScopeAndCommunityIdAndStatus(ReportScope scope, Integer communityId, ReportStatus status, Pageable pageable);

    /**
     * Finds chat reports by scope, university ID (context), and status.
     */
    Page<Report> findByScopeAndUniversityIdAndStatusAndConversationIdIsNotNull(ReportScope scope, Integer universityId, ReportStatus status, Pageable pageable);

}