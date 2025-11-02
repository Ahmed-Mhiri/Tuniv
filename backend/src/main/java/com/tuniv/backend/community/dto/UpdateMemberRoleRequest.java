package com.tuniv.backend.community.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull
        Integer userId,

        @NotNull
        Integer roleId
) {}