package com.tuniv.backend.chat.projection.reaction;

public interface ReactionSummaryProjection {
    Integer getMessageId();
    String getEmoji();
    Long getCount();
    Integer getConversationId(); // For trend analysis
}
