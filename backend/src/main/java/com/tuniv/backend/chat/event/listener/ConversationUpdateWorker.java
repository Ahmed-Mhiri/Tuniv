package com.tuniv.backend.chat.event.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuniv.backend.chat.dto.NewMessageEventDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConversationUpdateWorker {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String PENDING_UPDATES_KEY = "convo:last_message:pending";

    // Define queue name (must match configuration)
    public static final String QUEUE_NAME = "q.conversation.last_message_updater"; 

    @RabbitListener(queues = QUEUE_NAME)
    public void handleNewMessage(NewMessageEventDto event) {
        log.debug("Received NewMessageEvent for convo {}, msg {}", event.getConversationId(), event.getMessageId());
        try {
            // Store the entire event DTO as JSON in the Redis Hash.
            String eventJson = objectMapper.writeValueAsString(event);
            
            // HSET "convo:last_message:pending" "conversationId" "{eventJson}"
            redisTemplate.opsForHash().put(
                PENDING_UPDATES_KEY,
                event.getConversationId().toString(),
                eventJson 
            );
            log.debug("Updated pending last message in Redis for convo {}", event.getConversationId());
        } catch (Exception e) {
            log.error("Error processing NewMessageEvent for convo {}: {}", event.getConversationId(), e.getMessage(), e);
            // Consider adding retry logic or dead-letter queue handling here
        }
    }
}
