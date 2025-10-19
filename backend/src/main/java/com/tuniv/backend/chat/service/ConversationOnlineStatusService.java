package com.tuniv.backend.chat.service;

import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.chat.repository.ConversationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationOnlineStatusService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WebSocketSessionService webSocketSessionService;

    // In-memory cache for frequently accessed conversations
    private final Map<Integer, ConversationOnlineStatus> statusCache = new ConcurrentHashMap<>();

    // Redis keys
    private static final String CONVERSATION_ONLINE_KEY = "conversation:online:%d";
    private static final String CONVERSATION_ACTIVITY_KEY = "conversation:activity:%d";
    private static final String USER_LAST_ACTIVITY_KEY = "user:activity:%d";

    @Transactional
    public void updateUserOnlineStatus(Integer conversationId, Integer userId, boolean isOnline) {
        String onlineKey = String.format(CONVERSATION_ONLINE_KEY, conversationId);
        String activityKey = String.format(USER_LAST_ACTIVITY_KEY, userId);

        if (isOnline) {
            redisTemplate.opsForSet().add(onlineKey, userId.toString());
            redisTemplate.opsForValue().set(activityKey, Instant.now().toString());
        } else {
            redisTemplate.opsForSet().remove(onlineKey, userId.toString());
        }

        // Update cache
        updateCache(conversationId);
        
        log.debug("User {} {} in conversation {}", userId, isOnline ? "online" : "offline", conversationId);
    }

    @Transactional
    public void recordUserActivity(Integer conversationId, Integer userId) {
        String activityKey = String.format(CONVERSATION_ACTIVITY_KEY, conversationId);
        String userActivityKey = String.format(USER_LAST_ACTIVITY_KEY, userId);

        Instant now = Instant.now();
        redisTemplate.opsForZSet().add(activityKey, userId.toString(), now.toEpochMilli());
        redisTemplate.opsForValue().set(userActivityKey, now.toString());

        // Update conversation last activity
        conversationRepository.findById(conversationId).ifPresent(conversation -> {
            conversation.setLastActivityAt(now);
            conversationRepository.save(conversation);
        });

        updateCache(conversationId);
    }

    @Transactional(readOnly = true)
    public ConversationOnlineStatus getOnlineStatus(Integer conversationId) {
        // Try cache first
        ConversationOnlineStatus cachedStatus = statusCache.get(conversationId);
        if (cachedStatus != null && cachedStatus.isValid()) {
            return cachedStatus;
        }

        // Build from Redis
        return buildOnlineStatus(conversationId);
    }

    private ConversationOnlineStatus buildOnlineStatus(Integer conversationId) {
        String onlineKey = String.format(CONVERSATION_ONLINE_KEY, conversationId);
        String activityKey = String.format(CONVERSATION_ACTIVITY_KEY, conversationId);

        Set<Object> onlineUserIds = redisTemplate.opsForSet().members(onlineKey);
        Set<Object> recentlyActiveUserIds = redisTemplate.opsForZSet().rangeByScore(
            activityKey, 
            Instant.now().minus(15, ChronoUnit.MINUTES).toEpochMilli(),
            Double.MAX_VALUE
        );

        ConversationOnlineStatus status = new ConversationOnlineStatus();
        status.setConversationId(conversationId);
        status.setOnlineCount(onlineUserIds != null ? onlineUserIds.size() : 0);
        status.setRecentlyActiveCount(recentlyActiveUserIds != null ? recentlyActiveUserIds.size() : 0);
        status.setLastUpdated(Instant.now());

        // Cache the result
        statusCache.put(conversationId, status);

        return status;
    }

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    @Transactional
    public void updateConversationStatistics() {
        log.debug("Updating conversation online statistics");
        
        List<Conversation> activeConversations = conversationRepository.findByIsActiveTrue();
        
        for (Conversation conversation : activeConversations) {
            try {
                updateConversationStats(conversation);
            } catch (Exception e) {
                log.error("Failed to update stats for conversation {}: {}", 
                         conversation.getConversationId(), e.getMessage());
            }
        }
    }

    private void updateConversationStats(Conversation conversation) {
        ConversationOnlineStatus status = getOnlineStatus(conversation.getConversationId());
        
        conversation.setOnlineParticipantCount(status.getOnlineCount());
        conversation.setRecentActiveParticipantCount(status.getRecentlyActiveCount());
        
        // Update cached admin IDs for large groups
        if (conversation.isLargeGroup() || 
            conversation.getParticipantCount() > conversation.getParticipantCountThreshold()) {
            updateCachedAdminIds(conversation);
        }
        
        conversationRepository.save(conversation);
    }

    private void updateCachedAdminIds(Conversation conversation) {
        List<ConversationParticipant> admins = participantRepository.findActiveParticipantsByRole(
            conversation.getConversationId(), "CONVERSATION_ADMIN");
        
        List<Integer> adminIds = admins.stream()
            .map(participant -> participant.getUser().getUserId())
            .collect(Collectors.toList());
        
        // Store as JSON string
        conversation.setCachedAdminIds(adminIds.toString());
        conversation.setSummaryUpdatedAt(Instant.now());
    }

    private void updateCache(Integer conversationId) {
        statusCache.remove(conversationId); // Invalidate cache
    }

    // DTO for online status
    @Getter
    @Setter
    public static class ConversationOnlineStatus {
        private Integer conversationId;
        private Integer onlineCount;
        private Integer recentlyActiveCount;
        private Instant lastUpdated;

        public boolean isValid() {
            return lastUpdated != null && 
                   lastUpdated.isAfter(Instant.now().minus(2, ChronoUnit.MINUTES));
        }
    }
}