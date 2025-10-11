package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class UserPresenceDto {
    private Integer userId;
    private Integer conversationId;
    private boolean isOnline;
    private Instant updatedAt;

    public UserPresenceDto(Integer userId, Integer conversationId, boolean isOnline, Instant updatedAt) {
        this.userId = userId;
        this.conversationId = conversationId;
        this.isOnline = isOnline;
        this.updatedAt = updatedAt;
    }
}