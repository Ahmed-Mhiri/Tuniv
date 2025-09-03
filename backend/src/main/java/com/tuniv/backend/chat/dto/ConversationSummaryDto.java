package com.tuniv.backend.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConversationSummaryDto {
    private Integer conversationId;
    private Integer participantId; // ✅ ADD THIS FIELD
    private String participantName;
    private String participantAvatarUrl;
    private String lastMessage;
    private String lastMessageTimestamp;
    private int unreadCount;
}
