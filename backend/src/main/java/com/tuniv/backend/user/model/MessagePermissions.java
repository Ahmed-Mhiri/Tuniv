package com.tuniv.backend.user.model;


    public enum MessagePermissions {
        ANYONE,         // Anyone can message
        FOLLOWERS,      // Only followers can message
        UNIVERSITY,     // Only university members
        NONE           // No messages allowed
    }
