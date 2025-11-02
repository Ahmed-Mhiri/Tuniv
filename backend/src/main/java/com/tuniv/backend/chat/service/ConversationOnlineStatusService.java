package com.tuniv.backend.chat.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.chat.dto.common.ConversationOnlineStatus;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.chat.repository.ConversationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationOnlineStatusService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WebSocketSessionService webSocketSessionService;

    // Redis keys - aligned with WebSocketSessionService
    private static final String CONVERSATION_ONLINE_KEY = "conversation:active:users:%d"; // Now using same key pattern
    private static final String CONVERSATION_ACTIVITY_KEY = "conversation:activity:%d";
    private static final String USER_LAST_ACTIVITY_KEY = "user:activity:%d"; // Consistent with WebSocketSessionService

    @Transactional
    public void updateUserOnlineStatus(Integer conversationId, Integer userId, boolean isOnline) {
        // Use WebSocketSessionService for online status tracking since it's the source of truth
        // This method now becomes a wrapper that coordinates between services
        
        if (isOnline) {
            // WebSocketSessionService already handles adding to conversation:active:users:%d
            // We just need to record the activity timestamp
            recordUserActivity(conversationId, userId);
        } else {
            // WebSocketSessionService handles removal from active users
            // We clean up our activity tracking
            String activityKey = String.format(CONVERSATION_ACTIVITY_KEY, conversationId);
            redisTemplate.opsForZSet().remove(activityKey, userId.toString());
        }
        
        log.debug("User {} {} in conversation {}", userId, isOnline ? "online" : "offline", conversationId);
    }

    @Transactional
    public void recordUserActivity(Integer conversationId, Integer userId) {
        String activityKey = String.format(CONVERSATION_ACTIVITY_KEY, conversationId);
        
        Instant now = Instant.now();
        // Record user activity in this conversation with timestamp
        redisTemplate.opsForZSet().add(activityKey, userId.toString(), now.toEpochMilli());
        
        // Set TTL for automatic cleanup (24 hours for activity history)
        redisTemplate.expire(activityKey, Duration.ofHours(24));
        
        // Update conversation last activity in database
        conversationRepository.findById(conversationId).ifPresent(conversation -> {
            conversation.setLastActivityAt(now);
            conversationRepository.save(conversation);
        });
        
        log.debug("Recorded activity for user {} in conversation {}", userId, conversationId);
    }

    @Transactional(readOnly = true)
    public ConversationOnlineStatus getOnlineStatus(Integer conversationId) {
        // Build fresh from Redis every time - no caching to ensure consistency across instances
        return buildOnlineStatus(conversationId);
    }

    private ConversationOnlineStatus buildOnlineStatus(Integer conversationId) {
        // Get online users from WebSocketSessionService (source of truth)
        List<Integer> onlineUserIds = webSocketSessionService.getActiveUsersInConversation(conversationId);
        
        // Get recently active users from our activity tracking
        String activityKey = String.format(CONVERSATION_ACTIVITY_KEY, conversationId);
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
        
        log.debug("Updated stats for conversation {}: online={}, recentlyActive={}", 
                 conversation.getConversationId(), status.getOnlineCount(), status.getRecentlyActiveCount());
    }

    private void updateCachedAdminIds(Conversation conversation) {
        List<ConversationParticipant> admins = participantRepository.findByConversationIdAndRoleNameAndIsActiveTrue(
            conversation.getConversationId(), "CONVERSATION_ADMIN");
        
        List<Integer> adminIds = admins.stream()
            .map(participant -> participant.getUser().getUserId())
            .collect(Collectors.toList());
        
        // Store as JSON string
        conversation.setCachedAdminIds(adminIds.toString());
        conversation.setSummaryUpdatedAt(Instant.now());
    }

    // ========== Additional Utility Methods ==========
    
    /**
     * Gets detailed online status for a conversation
     */
    public Map<String, Object> getDetailedOnlineStatus(Integer conversationId) {
        Map<String, Object> detailedStatus = new HashMap<>();
        
        ConversationOnlineStatus basicStatus = getOnlineStatus(conversationId);
        detailedStatus.put("basic", basicStatus);
        
        // Get actual user IDs for online users
        List<Integer> onlineUsers = webSocketSessionService.getActiveUsersInConversation(conversationId);
        detailedStatus.put("onlineUsers", onlineUsers);
        
        // Get recently active user IDs
        String activityKey = String.format(CONVERSATION_ACTIVITY_KEY, conversationId);
        Set<Object> recentActiveUsers = redisTemplate.opsForZSet().rangeByScore(
            activityKey, 
            Instant.now().minus(15, ChronoUnit.MINUTES).toEpochMilli(),
            Double.MAX_VALUE
        );
        
        List<Integer> recentUserIds = recentActiveUsers != null ? 
            recentActiveUsers.stream()
                .map(obj -> Integer.parseInt(obj.toString()))
                .collect(Collectors.toList()) : 
            Collections.emptyList();
        
        detailedStatus.put("recentlyActiveUsers", recentUserIds);
        
        return detailedStatus;
    }
    
    /**
     * Clean up old activity records (run periodically)
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupOldActivityRecords() {
        log.info("Cleaning up old conversation activity records");
        
        List<Conversation> conversations = conversationRepository.findByIsActiveTrue();
        for (Conversation conversation : conversations) {
            String activityKey = String.format(CONVERSATION_ACTIVITY_KEY, conversation.getConversationId());
            
            // Remove activity records older than 7 days
            double maxScore = Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli();
            Long removed = redisTemplate.opsForZSet().removeRangeByScore(activityKey, 0, maxScore);
            
            if (removed != null && removed > 0) {
                log.debug("Removed {} old activity records for conversation {}", 
                         removed, conversation.getConversationId());
            }
        }
    }

    // DTO for online status
    
}