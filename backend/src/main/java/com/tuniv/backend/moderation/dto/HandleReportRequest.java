package com.tuniv.backend.moderation.dto;

import com.tuniv.backend.moderation.model.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HandleReportRequest(
    @NotNull
    ReportStatus newStatus, // e.g., RESOLVED, DISMISSED

    @Size(max = 5000)
    String moderatorNotes,

    // The action taken, which will be recorded in the moderation log
    ModerationActionDto action
) {}