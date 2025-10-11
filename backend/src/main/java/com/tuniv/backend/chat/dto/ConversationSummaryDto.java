package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
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