package com.tuniv.backend.auth.dto;

import com.tuniv.backend.university.model.UniversityRole;

import com.tuniv.backend.university.model.VerificationStatus;

public record VerificationInfo(
    String universityName,
    VerificationStatus status,
    UniversityRole role
) {}
