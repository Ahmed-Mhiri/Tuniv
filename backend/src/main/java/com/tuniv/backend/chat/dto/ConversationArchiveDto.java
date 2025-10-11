package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class ConversationArchiveDto {
    private Integer conversationId;
    private boolean archived;
    private Instant updatedAt;

    public ConversationArchiveDto(Integer conversationId, boolean archived, Instant updatedAt) {
        this.conversationId = conversationId;
        this.archived = archived;
        this.updatedAt = updatedAt;
    }
}