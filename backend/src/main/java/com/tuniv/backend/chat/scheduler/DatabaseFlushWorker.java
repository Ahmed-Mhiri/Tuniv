package com.tuniv.backend.chat.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuniv.backend.chat.dto.NewMessageEventDto;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.repository.ConversationRepository;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseFlushWorker {

    private final RedisTemplate<String, String> redisTemplate;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    
    private static final String PENDING_UPDATES_KEY = "convo:last_message:pending";

    // Run every 2 seconds (adjust as needed)
    @Scheduled(fixedDelayString = "${app.chat.last-message-flush-interval:2000}") 
    @Transactional
    public void flushUpdatesToDb() {
        // Atomically get all pending updates (as Map<String, String>) and delete the key
        Map<Object, Object> pendingUpdatesRaw = redisTemplate.opsForHash().entries(PENDING_UPDATES_KEY);
        if (pendingUpdatesRaw.isEmpty()) {
            return; // Nothing to do
        }
        redisTemplate.delete(PENDING_UPDATES_KEY);
        log.debug("Flushing {} pending conversation updates to DB", pendingUpdatesRaw.size());

        List<NewMessageEventDto> events = new ArrayList<>();
        List<Integer> conversationIds = new ArrayList<>();
        List<Integer> authorIds = new ArrayList<>();

        // 1. Deserialize events and collect IDs
        for (Map.Entry<Object, Object> entry : pendingUpdatesRaw.entrySet()) {
            try {
                String eventJson = (String) entry.getValue();
                NewMessageEventDto event = objectMapper.readValue(eventJson, NewMessageEventDto.class);
                events.add(event);
                conversationIds.add(event.getConversationId());
                if (event.getAuthorId() != null) {
                    authorIds.add(event.getAuthorId());
                }
            } catch (Exception e) {
                log.error("Failed to deserialize pending update event for key {}: {}", entry.getKey(), e.getMessage());
            }
        }
        
        if (events.isEmpty()) return;

        // 2. Bulk fetch necessary entities
        Map<Integer, Conversation> conversationsMap = conversationRepository.findAllById(conversationIds).stream()
                .collect(Collectors.toMap(Conversation::getConversationId, Function.identity()));
        
        Map<Integer, User> authorsMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        List<Conversation> conversationsToUpdate = new ArrayList<>();

        // 3. Process updates
        for (NewMessageEventDto event : events) {
            Conversation conversation = conversationsMap.get(event.getConversationId());
            if (conversation == null) {
                log.warn("Conversation {} not found during flush, skipping update.", event.getConversationId());
                continue;
            }

            // Check if this event is still the latest one for the conversation
            if (conversation.getLastMessageSentAt() == null || event.getSentAt().isAfter(conversation.getLastMessageSentAt())) {
                conversation.setLastMessageBody(event.getBodyPreview());
                conversation.setLastMessageSentAt(event.getSentAt());
                
                // Fetch author User entity from the map
                User author = (event.getAuthorId() != null) ? authorsMap.get(event.getAuthorId()) : null;
                conversation.setLastMessageAuthor(author); 
                
                conversationsToUpdate.add(conversation);
            }
        }
        
        // 4. Bulk save updated conversations
        if (!conversationsToUpdate.isEmpty()) {
            conversationRepository.saveAll(conversationsToUpdate);
            log.info("Successfully flushed last message updates for {} conversations.", conversationsToUpdate.size());
        }
    }
}