// ConversationDetailDto.java
package com.tuniv.backend.chat.dto;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDetailDto {
    private Integer conversationId;
    private String title;
    private String conversationType;
    private Integer universityContextId;
    private String lastMessageBody;
    private Instant lastMessageSentAt;
    private Integer lastMessageAuthorId;
    private String lastMessageAuthorName;
    private Integer messageCount;
    private Integer participantCount;
    private boolean isActive;
    private boolean isArchived;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ParticipantDto> participants;
    private List<PinnedMessageDto> pinnedMessages;
    private ParticipantDto currentUserParticipant;
}