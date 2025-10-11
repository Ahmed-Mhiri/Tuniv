package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class UnreadCountDto {
    private Integer conversationId;
    private Integer unreadCount;
    private Instant lastReadTimestamp;
}