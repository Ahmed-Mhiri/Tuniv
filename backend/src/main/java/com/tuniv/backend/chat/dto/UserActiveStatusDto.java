package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class UserActiveStatusDto {
    private Integer userId;
    private Integer conversationId;
    private boolean isActive;
    private Instant updatedAt;

    public UserActiveStatusDto(Integer userId, Integer conversationId, boolean isActive, Instant updatedAt) {
        this.userId = userId;
        this.conversationId = conversationId;
        this.isActive = isActive;
        this.updatedAt = updatedAt;
    }
}

