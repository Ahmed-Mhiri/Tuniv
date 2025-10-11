package com.tuniv.backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class JwtResponse {

    // --- Core Identity & Token ---
    private String token;
    private Integer id;
    private String username;
    private String email;

    // --- Profile Information ---
    private String profilePhotoUrl;
    private String bio;
    private String major;
    private int reputationScore;
    private VerificationInfo verificationInfo;

    // --- Activity Statistics ---
    private int topicsCount;
    private int repliesCount;
    private int followersCount;
    private int followingCount;

    // --- UI State Counts ---
    private long unreadNotificationsCount;
    private long unreadMessagesCount;

    // --- Security ---
    private boolean is2faRequired;
    private boolean is2faEnabled;
    
    // Add no-args constructor for serialization
    public JwtResponse() {}
}