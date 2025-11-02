// ConversationSummaryDto.java
package com.tuniv.backend.chat.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryDto {
    private Integer conversationId;
    private String title;
    private String conversationType;
    private String lastMessageBody;
    private Instant lastMessageSentAt;
    private Integer lastMessageAuthorId;
    private String lastMessageAuthorName;
    private Integer unreadCount;
    private Integer participantCount;
    private boolean isArchived;
    private Instant updatedAt;
}