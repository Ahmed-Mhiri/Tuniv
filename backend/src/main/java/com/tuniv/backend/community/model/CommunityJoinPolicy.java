package com.tuniv.backend.community.model;

    // ========== ENUMS ==========
    public enum CommunityJoinPolicy {
        OPEN,           // Anyone can join
        APPROVAL,       // Requires moderator approval
        RESTRICTED,     // Invitation only
        UNIVERSITY_ONLY // Only university members
    }