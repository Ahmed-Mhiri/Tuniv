package com.tuniv.backend.community.dto;

import jakarta.validation.constraints.Size;

import java.util.Set;

public record RoleUpdateRequest(
        @Size(min = 3, max = 100)
        String displayName,

        Set<String> permissions
) {}