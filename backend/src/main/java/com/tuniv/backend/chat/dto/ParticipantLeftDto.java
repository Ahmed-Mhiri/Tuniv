package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class ParticipantLeftDto {
    private Integer userId;
    private String username;
    private Integer conversationId;
    private Instant leftAt;

    public ParticipantLeftDto(Integer userId, String username, Integer conversationId, Instant leftAt) {
        this.userId = userId;
        this.username = username;
        this.conversationId = conversationId;
        this.leftAt = leftAt;
    }
}