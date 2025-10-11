package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageStatsDto {
    private Integer conversationId;
    private Integer totalMessages;
    private Long pinnedMessagesCount; // Change from Integer to Long
    private Integer todayMessageCount;
    private Integer activeParticipantsCount;
    private Double averageMessagesPerDay;
}
