// StopTypingDto.java
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
public class StopTypingDto {
    private Integer userId;
    private Integer conversationId;
    private Instant stoppedAt;
}