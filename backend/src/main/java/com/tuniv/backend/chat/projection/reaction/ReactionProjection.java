package com.tuniv.backend.chat.projection.reaction;

import java.time.Instant;

public interface ReactionProjection {
    Integer getId();
    Integer getMessageId();
    Integer getUserId();
    String getUsername();
    String getProfilePhotoUrl();
    String getEmoji();
    Integer getSkinTone();
    String getCustomText();
    Instant getCreatedAt();
    Boolean getIsRemoved();
    Instant getRemovedAt();
    // For analytics
    Long getReactionCount();
}