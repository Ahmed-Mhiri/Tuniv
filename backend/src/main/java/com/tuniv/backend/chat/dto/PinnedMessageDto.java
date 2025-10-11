package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class PinnedMessageDto {
    private Integer id;
    private String body;
    private Integer authorId;
    private String authorUsername;
    private Integer conversationId;
    private Instant sentAt;
    private Instant pinnedAt;
    private Integer pinnedByUserId;
    private String pinnedByUsername;
}