package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class BulkMessageDeletionDto {
    private List<Integer> messageIds;
    private Integer conversationId;
    private Instant deletedAt;

    public BulkMessageDeletionDto(List<Integer> messageIds, Integer conversationId, Instant deletedAt) {
        this.messageIds = messageIds;
        this.conversationId = conversationId;
        this.deletedAt = deletedAt;
    }
}
