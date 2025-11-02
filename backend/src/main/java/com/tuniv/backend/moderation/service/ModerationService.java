package com.tuniv.backend.moderation.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tuniv.backend.moderation.dto.CreateReportRequest;
import com.tuniv.backend.moderation.dto.HandleReportRequest;
import com.tuniv.backend.moderation.dto.ReportDetailDto;
import com.tuniv.backend.moderation.dto.ReportSummaryDto;
import com.tuniv.backend.moderation.model.ReportScope;
import com.tuniv.backend.user.model.User;

public interface ModerationService {

    /**
     * Creates a new report for a piece of content or a user.
     *
     * @param request  The DTO containing report details.
     * @param reporter The user filing the report.
     * @return A DTO of the newly created report.
     */
    ReportSummaryDto createReport(CreateReportRequest request, User reporter);

    /**
     * Handles a report by updating its status (e.g., RESOLVED, DISMISSED).
     *
     * @param reportId  The ID of the report to handle.
     * @param request   The DTO containing resolution details.
     * @param moderator The moderator handling the report.
     * @return A DTO of the updated report.
     */
    ReportDetailDto handleReport(Integer reportId, HandleReportRequest request, User moderator);

    /**
     * Gets a paginated list of reports for a specific moderation queue.
     *
     * @param scope        The scope to query (PLATFORM, UNIVERSITY, COMMUNITY, CHAT).
     * @param universityId Optional university ID (for UNIVERSITY and CHAT scopes).
     * @param communityId  Optional community ID (for COMMUNITY scope).
     * @param pageable     Pagination information.
     * @return A page of report summaries.
     */
    Page<ReportSummaryDto> getReportsByScope(ReportScope scope, Integer universityId, Integer communityId, Pageable pageable);

    /**
     * Gets the detailed view of a single report.
     *
     * @param reportId The ID of the report.
     * @return A detailed DTO of the report.
     */
    ReportDetailDto getReportDetails(Integer reportId);
}