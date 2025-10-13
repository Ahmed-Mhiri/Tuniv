// MessageDeletionDto.java
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
public class MessageDeletionDto {
    private Integer messageId;
    private Integer conversationId;
    private Instant deletedAt;
    private Integer deletedByUserId;
    private String deletedByUsername;

    // ✅ ADD THIS CONSTRUCTOR
    public MessageDeletionDto(Integer messageId, Integer conversationId, Instant deletedAt) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.deletedAt = deletedAt;
    }
}