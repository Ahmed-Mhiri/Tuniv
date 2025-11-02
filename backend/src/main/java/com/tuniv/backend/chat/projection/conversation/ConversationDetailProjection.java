// ConversationDetailProjection.java
package com.tuniv.backend.chat.projection.conversation;

import java.time.Instant;

public interface ConversationDetailProjection {
    Integer getConversationId();
    String getTitle();
    String getConversationType();
    Integer getUniversityContextId();
    String getLastMessageBody();
    Instant getLastMessageSentAt();
    Integer getLastMessageAuthorId();
    Integer getMessageCount();
    Integer getParticipantCount();
    Boolean getIsActive();
    Boolean getIsArchived();
    Instant getCreatedAt();
    Instant getUpdatedAt();
    Integer getOnlineParticipantCount();
    Integer getRecentlyActiveParticipantCount();
    Instant getLastActivityAt();
    Boolean getIsLargeGroup();
    
    // Add default method for conversion
    default com.tuniv.backend.chat.dto.response.ConversationDetailDto toDetailDto() {
        com.tuniv.backend.chat.dto.response.ConversationDetailDto dto = new com.tuniv.backend.chat.dto.response.ConversationDetailDto();
        dto.setConversationId(getConversationId());
        dto.setTitle(getTitle());
        dto.setConversationType(getConversationType());
        dto.setUniversityContextId(getUniversityContextId());
        dto.setLastMessageBody(getLastMessageBody());
        dto.setLastMessageSentAt(getLastMessageSentAt());
        dto.setLastMessageAuthorId(getLastMessageAuthorId());
        dto.setMessageCount(getMessageCount());
        dto.setParticipantCount(getParticipantCount());
        dto.setActive(getIsActive());
        dto.setArchived(getIsArchived());
        dto.setCreatedAt(getCreatedAt());
        dto.setUpdatedAt(getUpdatedAt());
        dto.setOnlineParticipantCount(getOnlineParticipantCount());
        dto.setRecentlyActiveParticipantCount(getRecentlyActiveParticipantCount());
        dto.setLastActivityAt(getLastActivityAt());
        dto.setLargeGroup(getIsLargeGroup());
        return dto;
    }
}