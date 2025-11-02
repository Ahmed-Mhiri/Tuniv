package com.tuniv.backend.chat.dto.common;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConversationOnlineStatus {
        private Integer conversationId;
        private Integer onlineCount;
        private Integer recentlyActiveCount;
        private Instant lastUpdated;

}