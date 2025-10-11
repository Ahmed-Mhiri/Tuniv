package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class MessageDeletionDto {
    private Integer messageId;
    private Integer conversationId;
    private Instant deletedAt;
    private Integer deletedByUserId;
    private String deletedByUsername;

    public MessageDeletionDto(Integer messageId, Integer conversationId, Instant deletedAt) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.deletedAt = deletedAt;
    }
}
