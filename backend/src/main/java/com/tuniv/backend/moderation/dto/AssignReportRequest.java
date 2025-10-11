package com.tuniv.backend.moderation.dto;

import jakarta.validation.constraints.NotNull;

public record AssignReportRequest(
    @NotNull
    Integer moderatorId
) {}