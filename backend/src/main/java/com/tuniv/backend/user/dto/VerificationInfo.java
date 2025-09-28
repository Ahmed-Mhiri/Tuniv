package com.tuniv.backend.user.dto;

import com.tuniv.backend.university.model.UserRoleEnum;

public record VerificationInfo(
    String universityName,
    UserRoleEnum role
) {}
