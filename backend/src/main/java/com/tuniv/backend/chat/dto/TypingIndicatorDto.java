package com.tuniv.backend.chat.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TypingIndicatorDto {
    private Integer userId;
    private String username;
    private String profilePhotoUrl;
    private Integer conversationId;
    private Instant startedAt;
    private boolean isTyping;
    
    // Add constructor for convenience
    public TypingIndicatorDto(Integer userId, String username, Integer conversationId) {
        this.userId = userId;
        this.username = username;
        this.conversationId = conversationId;
        this.startedAt = Instant.now();
        this.isTyping = true;
    }
}