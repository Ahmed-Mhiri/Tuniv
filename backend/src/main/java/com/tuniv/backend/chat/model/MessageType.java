package com.tuniv.backend.chat.model;

public enum MessageType {
    TEXT,
    IMAGE,
    FILE,
    SYSTEM,     // <-- This is the key value
    ANNOUNCEMENT,
    POLL,
    EVENT,
    EMOJI
}