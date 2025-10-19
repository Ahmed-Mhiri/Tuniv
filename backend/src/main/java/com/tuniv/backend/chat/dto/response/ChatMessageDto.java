package com.tuniv.backend.chat.dto;


import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ChatMessageDto {
    private Integer id;
    private String body;
    private Integer authorId;
    private String authorUsername;
    private String authorProfilePhotoUrl;
    private Integer conversationId;
    private Instant sentAt;
    private Instant editedAt;
    private boolean isEdited;
    private Integer editCount;
    private String messageType;
    private boolean isDeleted;
    private Instant deletedAt;
    private String deletionReason;
    private boolean isPinned;
    private String clientMessageId;
    
    // Reply information
    private Integer replyToMessageId;
    private String replyToMessageBody;
    private Integer replyToAuthorId;
    private String replyToAuthorUsername;
    
    // Reactions - ONLY SUMMARY, no detailed list
    private MessageReactionsSummaryDto reactionsSummary;
    
    // Read status
    private boolean isReadByCurrentUser;
    private Integer readByCount; // ✅ ADDED: Simple count instead of detailed list

    public ChatMessageDto() {
        this.reactionsSummary = new MessageReactionsSummaryDto();
        // ❌ REMOVED: this.readBy = new ArrayList<>();
    }
}