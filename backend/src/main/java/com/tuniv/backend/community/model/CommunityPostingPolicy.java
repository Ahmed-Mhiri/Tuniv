package com.tuniv.backend.community.model;


    public enum CommunityPostingPolicy {
        MEMBERS,        // Only community members
        ANY_VERIFIED,   // Any verified student
        UNIVERSITY_MEMBERS, // Only university members
        MODERATORS_ONLY // Only moderators
    }

