// ConversationListProjection.java
package com.tuniv.backend.chat.projection.conversation;

import java.time.Instant;

public interface ConversationListProjection {
    Integer getConversationId();
    String getTitle();
    String getConversationType();
    String getLastMessageBody();
    Instant getLastMessageSentAt();
    Integer getLastMessageAuthorId();
    Integer getUnreadCount();
    Integer getParticipantCount();
    Boolean getIsArchived();
    Instant getUpdatedAt();
    
    // Add default method for conversion
    default com.tuniv.backend.chat.dto.response.ConversationSummaryDto toSummaryDto() {
        com.tuniv.backend.chat.dto.response.ConversationSummaryDto dto = new com.tuniv.backend.chat.dto.response.ConversationSummaryDto();
        dto.setConversationId(getConversationId());
        dto.setTitle(getTitle());
        dto.setConversationType(getConversationType());
        dto.setLastMessageBody(getLastMessageBody());
        dto.setLastMessageSentAt(getLastMessageSentAt());
        dto.setLastMessageAuthorId(getLastMessageAuthorId());
        dto.setUnreadCount(getUnreadCount());
        dto.setParticipantCount(getParticipantCount());
        dto.setArchived(getIsArchived());
        dto.setUpdatedAt(getUpdatedAt());
        return dto;
    }
}