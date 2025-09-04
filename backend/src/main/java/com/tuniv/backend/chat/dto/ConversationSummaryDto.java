package com.tuniv.backend.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor // ✅ FIX: Add a no-args constructor (good practice for DTOs)
@AllArgsConstructor // ✅ FIX: Add the all-arguments constructor that JPA needs
public class ConversationSummaryDto {

    private Integer conversationId;
    private Integer participantId;
    private String participantName;
    private String participantAvatarUrl;
    private String lastMessage;
    private String lastMessageTimestamp;

    // ✅ FIX: Change the type from int to Long to match the query's COUNT() result
    private Long unreadCount;

}