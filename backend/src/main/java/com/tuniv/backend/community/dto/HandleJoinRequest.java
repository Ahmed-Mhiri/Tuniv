package com.tuniv.backend.community.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HandleJoinRequest(
    @NotNull
    boolean approve,

    // Optional: Required if approve is false
    @Size(max = 500)
    String rejectionReason
) {}
