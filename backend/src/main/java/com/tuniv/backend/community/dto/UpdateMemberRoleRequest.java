package com.tuniv.backend.community.dto;

import com.tuniv.backend.community.model.CommunityRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
    @NotNull
    CommunityRole newRole
) {}