// MessageStatsDto.java
package com.tuniv.backend.chat.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatsDto {
    private Integer conversationId;
    private Integer totalMessages;
    private Long pinnedMessagesCount;
    private Integer todayMessageCount;
    private Integer activeParticipantsCount;
    private Double averageMessagesPerDay;
}