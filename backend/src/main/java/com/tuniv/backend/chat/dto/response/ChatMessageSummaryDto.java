// ChatMessageSummaryDto.java
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
public class ChatMessageSummaryDto {
    private Integer id;
    private String body;
    private Integer authorId;
    private String authorUsername;
    private String authorProfilePhotoUrl;
    private Integer conversationId;
    private Instant sentAt;
    private boolean isEdited;
    private String messageType;
    private boolean isPinned;
    
    // Reply information (basic)
    private Integer replyToMessageId;
    private String replyToMessageBody;
    
    // Reactions summary only (no detailed list)
    private MessageReactionsSummaryDto reactionsSummary;
    
    // Read status (basic)
    private boolean isReadByCurrentUser;
}