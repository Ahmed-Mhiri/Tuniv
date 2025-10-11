package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class TypingIndicatorDto {
    private Integer userId;
    private String username;
    private String profilePhotoUrl;
    private Integer conversationId;
    private Instant startedAt;
    private boolean isTyping; // true for start, false for stop
}