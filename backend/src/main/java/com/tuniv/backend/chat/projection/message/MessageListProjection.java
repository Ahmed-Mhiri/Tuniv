package com.tuniv.backend.chat.projection.message;

import java.time.Instant;

public interface MessageListProjection {
    Integer getId();
    String getBody();
    Integer getAuthorId();
    String getAuthorUsername();
    Integer getConversationId();
    Instant getSentAt();
    Boolean getIsEdited();
    String getMessageType();
    Boolean getIsPinned();
    Integer getReplyToMessageId();
    String getReplyToMessageBody();
    Integer getReplyToAuthorId();
    String getReplyToAuthorUsername();
}