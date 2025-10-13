// TypingIndicatorDto.java
package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

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
}