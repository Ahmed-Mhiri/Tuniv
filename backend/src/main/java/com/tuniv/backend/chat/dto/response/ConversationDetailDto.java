package com.tuniv.backend.chat.dto.response;

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
    
    // ✅ ENHANCED: Online status information
    private Integer onlineParticipantCount;
    private Integer recentlyActiveParticipantCount;
    private Instant lastActivityAt;
    private boolean isLargeGroup;
    
    // ✅ OPTIMIZED: Participant summary for large groups
    private ParticipantSummaryDto participantSummary;
    private List<PinnedMessageDto> pinnedMessages;
    private ParticipantDto currentUserParticipant;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantSummaryDto {
        private List<ParticipantDto> admins; // Cached admin list
        private List<ParticipantDto> recentlyActiveUsers; // Limited list
        private boolean hasMoreParticipants; // Flag for frontend
        private String participantFetchUrl; // URL for full list
    }
}