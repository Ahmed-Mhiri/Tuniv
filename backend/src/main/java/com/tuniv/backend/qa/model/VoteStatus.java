package com.tuniv.backend.qa.model;

public enum VoteStatus {
    UPVOTE,
    DOWNVOTE;
    
    // Helper to safely convert from a String, returning null if the string is null
    public static VoteStatus fromString(String status) {
        return status == null ? null : VoteStatus.valueOf(status);
    }
}