package com.tuniv.backend.chat.dto.event;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class ConversationDeletionDto {
    private Integer conversationId;
    private Instant deletedAt;

    public ConversationDeletionDto(Integer conversationId, Instant deletedAt) {
        this.conversationId = conversationId;
        this.deletedAt = deletedAt;
    }
}