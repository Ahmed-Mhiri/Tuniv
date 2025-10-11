package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class RealtimeMessage<T> {
    private String type;           // Event type
    private T data;               // The actual payload
    private Instant timestamp;    // When the event occurred
    private String description;   // Human-readable description
    private String id;           // Unique ID for this message
    private Integer conversationId; // Optional: conversation context

    public RealtimeMessage() {
        this.timestamp = Instant.now();
        this.id = java.util.UUID.randomUUID().toString();
    }

    public RealtimeMessage(String type, T data, String description) {
        this();
        this.type = type;
        this.data = data;
        this.description = description;
    }
}