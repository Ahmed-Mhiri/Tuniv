package com.tuniv.backend.chat.projection.message;
import java.time.Instant;
import java.util.Map;

public interface MessageLightProjection {
    Integer getId();
    String getBody();
    Integer getAuthorId();
    String getAuthorUsername();
    Integer getConversationId();
    Instant getSentAt();
    Boolean getIsEdited();
    String getMessageType();
    Boolean getIsPinned();
    Map<String, Long> getReactionCounts();
    Integer getTotalReactions();
}