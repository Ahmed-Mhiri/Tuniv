package com.tuniv.backend.chat.projection.message;

import java.time.Instant;

// This projection fetches only what's needed for a PinnedMessageDto
public interface PinnedMessageProjection {
    Integer getId();
    String getBody();
    Instant getSentAt();
    String getMessageType();
    Integer getAuthorId();
    String getAuthorUsername();
    
    // Pinned-specific fields
    Instant getPinnedAt();
    Integer getPinnedByUserId();
    String getPinnedByUsername();
    
    // Reply fields
    Integer getReplyToMessageId();
    String getReplyToMessageBody();
    Integer getReplyToAuthorId();
    String getReplyToAuthorUsername();
}