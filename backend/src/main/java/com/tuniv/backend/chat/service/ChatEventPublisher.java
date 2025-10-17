package com.tuniv.backend.chat.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.tuniv.backend.chat.event.SystemMessageRequestedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishSystemMessageRequested(Integer conversationId, String messageText) {
        try {
            SystemMessageRequestedEvent event = new SystemMessageRequestedEvent(conversationId, messageText);
            eventPublisher.publishEvent(event);
            log.debug("Published system message event for conversation {}: {}", conversationId, messageText);
        } catch (Exception e) {
            log.error("Failed to publish system message event for conversation {}: {}", 
                     conversationId, e.getMessage(), e);
            throw new RuntimeException("Failed to publish system message event", e);
        }
    }
}