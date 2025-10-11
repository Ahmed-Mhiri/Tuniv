package com.tuniv.backend.university.model;

    public enum VerificationMethod {
        EMAIL_DOMAIN,    // Standard university email verification
        MANUAL_APPROVAL, // Admin manual verification
        STUDENT_ID,      // Student ID verification
        INVITATION       // Invited by university admin
    }