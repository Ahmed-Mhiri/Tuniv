package com.tuniv.backend.moderation.dto;

import com.tuniv.backend.moderation.model.ReportStatus;
import com.tuniv.backend.user.dto.UserSummaryDto;
import java.time.Instant;
import java.util.List;

public record ReportDetailDto(
    Integer reportId,
    ReportStatus status,
    UserSummaryDto reporter,
    String reason,
    String reporterComment,
    Instant createdAt,
    ReportedContentDto reportedContent,
    UserSummaryDto assignedModerator, // The moderator handling the case
    String moderatorNotes,
    List<ModerationLogDto> targetUserHistory // Past actions against the user who was reported
) {}