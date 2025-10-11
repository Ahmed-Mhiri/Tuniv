package com.tuniv.backend.university.model;

// ========== ENUMS ==========
    public enum ModulePostingPolicy {
        UNIVERSITY_MEMBERS,  // Only university members can post
        MODULE_MEMBERS,      // Only enrolled module members
        ANY_VERIFIED,        // Any verified student
        RESTRICTED           // Specific users only
    }