package com.tuniv.backend.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPresenceDto {
    private Integer userId;
    private String username; // Added username for frontend convenience
    private Integer conversationId;
    private boolean isOnline;
    private Instant updatedAt;
}