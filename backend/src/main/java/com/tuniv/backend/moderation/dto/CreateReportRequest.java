package com.tuniv.backend.moderation.dto;

import com.tuniv.backend.shared.model.ContainerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
    @NotNull
    Integer contentId,

    @NotNull
    ContainerType contentType, // e.g., TOPIC, REPLY, USER_PROFILE

    @NotBlank @Size(max = 255)
    String reason, // A predefined reason from a list, e.g., "SPAM", "HARASSMENT"

    @Size(max = 2000)
    String comment // Optional additional details from the user
) {}