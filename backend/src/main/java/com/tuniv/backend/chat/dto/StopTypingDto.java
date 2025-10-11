package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class StopTypingDto {
    private Integer userId;
    private Integer conversationId;
    private Instant stoppedAt;

    public StopTypingDto(Integer userId, Integer conversationId, Instant stoppedAt) {
        this.userId = userId;
        this.conversationId = conversationId;
        this.stoppedAt = stoppedAt;
    }
}