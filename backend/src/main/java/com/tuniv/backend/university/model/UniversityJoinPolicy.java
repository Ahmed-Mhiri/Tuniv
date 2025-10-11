package com.tuniv.backend.university.model;

 public enum UniversityJoinPolicy {
        OPEN,           // Any verified student can join automatically
        REQUEST,        // Requires approval (default)
        DOMAIN_LOCKED,  // Only students with matching email domain
        CLOSED          // Invitation only
    }
