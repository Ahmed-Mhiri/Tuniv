package com.tuniv.backend.chat.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class SystemMessageRequestedEvent {
    private final Integer conversationId;
    private final String messageText;
    private final Instant timestamp = Instant.now();
    private final String eventType = "SYSTEM_MESSAGE_REQUESTED";
}