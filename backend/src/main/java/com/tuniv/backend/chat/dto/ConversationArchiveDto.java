// ConversationArchiveDto.java
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
public class ConversationArchiveDto {
    private Integer conversationId;
    private boolean archived;
    private Instant updatedAt;
}