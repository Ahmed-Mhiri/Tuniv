// ChatNotificationDto.java
package com.tuniv.backend.chat.dto.event;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatNotificationDto {
    private String type;           // e.g., "MENTION", "REACTION", "NEW_CONVERSATION"
    private String title;
    private String message;
    private Instant timestamp = Instant.now(); // Initialize directly
    private Map<String, Object> data;     // Additional data
    private String actionUrl;      // URL for frontend navigation
    private boolean isImportant;   // Whether to show as high priority

    // Remove the explicit constructor - @NoArgsConstructor handles it
}