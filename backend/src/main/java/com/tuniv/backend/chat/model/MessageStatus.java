package com.tuniv.backend.chat.model;

public enum MessageStatus {
        SENT,       // Message sent but not delivered
        DELIVERED,  // Message delivered to recipients
        READ,       // Message read by recipients
        DELETED     // Message deleted
    }