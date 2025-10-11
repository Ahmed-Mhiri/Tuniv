package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.Map;

@Getter
@Setter
public class ChatNotificationDto {
    private String type;           // e.g., "MENTION", "REACTION", "NEW_CONVERSATION"
    private String title;
    private String message;
    private Instant timestamp;
    private Map<String, Object> data;     // Additional data
    private String actionUrl;      // URL for frontend navigation
    private boolean isImportant;   // Whether to show as high priority

    public ChatNotificationDto() {
        this.timestamp = Instant.now();
    }
}