package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
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
    
    // Reactions
    private List<ReactionDto> reactions;
    private MessageReactionsSummaryDto reactionsSummary;
    
    // Read status
    private boolean isReadByCurrentUser;
    private List<ReadReceiptDto> readBy;
}