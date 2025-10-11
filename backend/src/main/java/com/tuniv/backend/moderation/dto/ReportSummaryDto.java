package com.tuniv.backend.moderation.dto;

import com.tuniv.backend.moderation.model.ReportStatus;
import com.tuniv.backend.shared.model.ContainerType;
import java.time.Instant;

public record ReportSummaryDto(
    Integer reportId,
    ReportStatus status,
    ContainerType contentType,
    String reason,
    String reporterUsername,
    Instant createdAt
) {}