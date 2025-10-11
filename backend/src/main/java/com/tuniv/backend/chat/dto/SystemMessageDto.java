package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.Map;

@Getter
@Setter
public class SystemMessageDto {
    private String type;           // e.g., "USER_JOINED", "CONVERSATION_CREATED"
    private String message;        // Human-readable message
    private Instant timestamp;
    private Map<String, Object> metadata; // Additional context data
    private Integer conversationId;

    public SystemMessageDto() {
        this.timestamp = Instant.now();
    }
}