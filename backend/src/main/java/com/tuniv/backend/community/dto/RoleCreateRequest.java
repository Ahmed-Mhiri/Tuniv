package com.tuniv.backend.community.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoleCreateRequest(
    @NotBlank @Size(min = 3, max = 50)
    String name,

    @NotBlank @Size(min = 3, max = 100)
    String displayName,

    @NotNull
    Set<String> permissions
) {}