package com.tuniv.backend.chat.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import com.tuniv.backend.config.security.services.UserDetailsImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketSessionService {

    private final ChatRealtimeService chatRealtimeService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ConversationOnlineStatusService conversationOnlineStatusService;

    // Redis key patterns
    private static final String USER_SESSIONS_KEY = "user:sessions:%d";
    private static final String SESSION_USER_KEY = "session:user:%s";
    private static final String SESSION_CONVERSATIONS_KEY = "session:conversations:%s";
    private static final String USER_ACTIVE_CONVERSATIONS_KEY = "user:active:conversations:%d";
    private static final String CONVERSATION_ACTIVE_USERS_KEY = "conversation:active:users:%d";
    private static final String USER_PRESENCE_KEY = "user:presence:%d";
    private static final String USER_LAST_ACTIVITY_KEY = "user:activity:%d";

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();
        
        Integer userId = extractUserIdFromHeaders(headers);
        
        if (userId != null) {
            // Store session mapping in Redis only
            addUserSession(userId, sessionId);
            setSessionUser(sessionId, userId);
            initializeSessionConversations(sessionId);
            
            // Set user as globally online
            setUserOnline(userId, true);
            
            // Record global user activity
            recordUserActivity(userId);
            
            log.debug("User {} connected with session {}", userId, sessionId);
        } else {
            log.warn("WebSocket connection without user authentication: {}", sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();
        
        Integer userId = getUserIdFromSession(sessionId);
        if (userId != null) {
            // Remove from conversation subscriptions using Redis data
            Set<Integer> conversationIds = getSessionConversations(sessionId);
            if (conversationIds != null) {
                for (Integer conversationId : conversationIds) {
                    handleUserLeftConversation(conversationId, userId);
                }
            }
            
            // Remove session from Redis
            removeUserSession(userId, sessionId);
            removeSessionUser(sessionId);
            removeSessionConversations(sessionId);
            
            // Check if user has any remaining sessions
            if (getUserSessionCount(userId) == 0) {
                // User has no more active sessions - set globally offline
                setUserOnline(userId, false);
                cleanupUserPresence(userId);
            }
            
            log.debug("User {} disconnected from session {}", userId, sessionId);
        }
    }

    @EventListener
    public void handleSubscriptionEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();
        String destination = headers.getDestination();
        
        Integer userId = getUserIdFromSession(sessionId);
        if (userId != null && destination != null) {
            handleDestinationSubscription(userId, sessionId, destination, true);
        }
    }

    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();
        String destination = headers.getDestination();
        
        Integer userId = getUserIdFromSession(sessionId);
        if (userId != null && destination != null) {
            handleDestinationSubscription(userId, sessionId, destination, false);
        }
    }

    // ========== Conversation Activity Tracking Methods ==========

    public void recordConversationActivity(Integer conversationId, Integer userId) {
        try {
            conversationOnlineStatusService.recordUserActivity(conversationId, userId);
            log.debug("Recorded activity for user {} in conversation {}", userId, conversationId);
        } catch (Exception e) {
            log.error("Failed to record conversation activity for user {} in conversation {}: {}", 
                     userId, conversationId, e.getMessage(), e);
        }
    }

    public void updateUserPresence(Integer conversationId, Integer userId, boolean isOnline) {
        try {
            conversationOnlineStatusService.updateUserOnlineStatus(conversationId, userId, isOnline);
            log.debug("Updated presence for user {} in conversation {}: online={}", 
                     userId, conversationId, isOnline);
        } catch (Exception e) {
            log.error("Failed to update user presence for user {} in conversation {}: {}", 
                     userId, conversationId, e.getMessage(), e);
        }
    }

    // ========== Enhanced Activity Tracking ==========

    public void recordUserMessageActivity(Integer conversationId, Integer userId) {
        recordConversationActivity(conversationId, userId);
        recordUserActivity(userId);
        
        // Also update the user's last activity in the conversation-specific tracking
        updateConversationLastActivity(conversationId, userId);
    }

    public void recordUserTypingActivity(Integer conversationId, Integer userId) {
        recordConversationActivity(conversationId, userId);
        
        // Update typing indicator in Redis
        String typingKey = String.format("typing:conversation:%d:user:%d", conversationId, userId);
        redisTemplate.opsForValue().set(typingKey, "typing", Duration.ofSeconds(10));
    }

    // ========== Enhanced Presence Methods ==========

    public boolean isUserActiveInConversation(Integer conversationId, Integer userId) {
        String userActivityKey = String.format(USER_LAST_ACTIVITY_KEY, userId);
        String lastActivityStr = (String) redisTemplate.opsForValue().get(userActivityKey);
        
        if (lastActivityStr != null) {
            try {
                Instant lastActivity = Instant.parse(lastActivityStr);
                // Consider user active if they had activity in the last 5 minutes
                return lastActivity.isAfter(Instant.now().minus(5, java.time.temporal.ChronoUnit.MINUTES));
            } catch (Exception e) {
                log.warn("Failed to parse last activity for user {}: {}", userId, e.getMessage());
            }
        }
        
        return false;
    }

    public Map<Integer, Boolean> getUsersPresenceInConversation(Integer conversationId, List<Integer> userIds) {
        Map<Integer, Boolean> presenceMap = new HashMap<>();
        
        for (Integer userId : userIds) {
            boolean isOnline = isUserOnline(userId) && isUserActiveInConversation(conversationId, userId);
            presenceMap.put(userId, isOnline);
        }
        
        return presenceMap;
    }

    // ========== Private Helper Methods ==========

    private void handleDestinationSubscription(Integer userId, String sessionId, String destination, boolean isSubscribing) {
        if (destination.startsWith("/topic/conversations/")) {
            handleConversationSubscription(userId, sessionId, destination, isSubscribing);
        } else if (destination.startsWith("/user/queue/")) {
            handleUserQueueSubscription(userId, isSubscribing);
        }
    }

    private void handleConversationSubscription(Integer userId, String sessionId, String destination, boolean isSubscribing) {
        try {
            String[] parts = destination.split("/");
            Integer conversationId = Integer.parseInt(parts[parts.length - 1]);
            
            if (isSubscribing) {
                handleUserJoinedConversation(conversationId, userId, sessionId);
            } else {
                handleUserLeftConversation(conversationId, userId);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid conversation ID in destination: {}", destination);
        } catch (Exception e) {
            log.error("Error handling conversation subscription for user {}: {}", userId, e.getMessage(), e);
        }
    }

    private void handleUserJoinedConversation(Integer conversationId, Integer userId, String sessionId) {
        // Mark user as active in this conversation
        addUserActiveConversation(userId, conversationId);
        addConversationActiveUser(conversationId, userId);
        
        // Track in Redis session conversations
        addSessionConversation(sessionId, conversationId);
        
        // Update presence status
        updateUserPresence(conversationId, userId, true);
        
        // Record activity
        recordConversationActivity(conversationId, userId);
        
        // Broadcast presence update
        chatRealtimeService.broadcastUserPresence(conversationId, userId, true);
        
        log.debug("User {} joined conversation {}", userId, conversationId);
    }

    private void handleUserLeftConversation(Integer conversationId, Integer userId) {
        // Remove user from conversation's active users
        removeConversationActiveUser(conversationId, userId);
        removeUserActiveConversation(userId, conversationId);
        
        // Update presence status
        updateUserPresence(conversationId, userId, false);
        
        // Broadcast presence update
        chatRealtimeService.broadcastUserPresence(conversationId, userId, false);
        
        log.debug("User {} left conversation {}", userId, conversationId);
    }

    private void handleUserQueueSubscription(Integer userId, boolean isSubscribing) {
        if (isSubscribing) {
            log.debug("User {} subscribed to personal queue", userId);
        } else {
            log.debug("User {} unsubscribed from personal queue", userId);
        }
    }

    private void recordUserActivity(Integer userId) {
        String key = String.format(USER_LAST_ACTIVITY_KEY, userId);
        redisTemplate.opsForValue().set(key, Instant.now().toString(), Duration.ofMinutes(10));
    }

    private void updateConversationLastActivity(Integer conversationId, Integer userId) {
        String key = String.format("conversation:%d:user:%d:last_activity", conversationId, userId);
        redisTemplate.opsForValue().set(key, Instant.now().toString(), Duration.ofMinutes(15));
    }

    // ========== Redis Operations ==========

    private void addUserSession(Integer userId, String sessionId) {
        String key = String.format(USER_SESSIONS_KEY, userId);
        redisTemplate.opsForSet().add(key, sessionId);
        redisTemplate.expire(key, Duration.ofHours(24));
    }

    private void removeUserSession(Integer userId, String sessionId) {
        String key = String.format(USER_SESSIONS_KEY, userId);
        redisTemplate.opsForSet().remove(key, sessionId);
    }

    private long getUserSessionCount(Integer userId) {
        String key = String.format(USER_SESSIONS_KEY, userId);
        Long size = redisTemplate.opsForSet().size(key);
        return size != null ? size : 0;
    }

    private void setSessionUser(String sessionId, Integer userId) {
        String key = String.format(SESSION_USER_KEY, sessionId);
        redisTemplate.opsForValue().set(key, userId.toString(), Duration.ofHours(24));
    }

    private Integer getUserIdFromSession(String sessionId) {
        String key = String.format(SESSION_USER_KEY, sessionId);
        String userIdStr = (String) redisTemplate.opsForValue().get(key);
        if (userIdStr != null) {
            try {
                return Integer.parseInt(userIdStr);
            } catch (NumberFormatException e) {
                log.warn("Invalid user ID in session mapping: {}", userIdStr);
            }
        }
        return null;
    }

    private void removeSessionUser(String sessionId) {
        String key = String.format(SESSION_USER_KEY, sessionId);
        redisTemplate.delete(key);
    }

    private void initializeSessionConversations(String sessionId) {
        String key = String.format(SESSION_CONVERSATIONS_KEY, sessionId);
        redisTemplate.opsForSet().add(key, ""); // Initialize with empty value
        redisTemplate.expire(key, Duration.ofHours(24));
    }

    private void addSessionConversation(String sessionId, Integer conversationId) {
        String key = String.format(SESSION_CONVERSATIONS_KEY, sessionId);
        redisTemplate.opsForSet().add(key, conversationId.toString());
    }

    private void removeSessionConversation(String sessionId, Integer conversationId) {
        String key = String.format(SESSION_CONVERSATIONS_KEY, sessionId);
        redisTemplate.opsForSet().remove(key, conversationId.toString());
    }

    private Set<Integer> getSessionConversations(String sessionId) {
        String key = String.format(SESSION_CONVERSATIONS_KEY, sessionId);
        Set<Object> conversationIds = redisTemplate.opsForSet().members(key);
        Set<Integer> result = new HashSet<>();
        
        if (conversationIds != null) {
            for (Object convId : conversationIds) {
                try {
                    if (!convId.toString().isEmpty()) { // Skip empty initialization value
                        result.add(Integer.parseInt(convId.toString()));
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid conversation ID in session: {}", convId);
                }
            }
        }
        
        return result;
    }

    private void removeSessionConversations(String sessionId) {
        String key = String.format(SESSION_CONVERSATIONS_KEY, sessionId);
        redisTemplate.delete(key);
    }

    private void setUserOnline(Integer userId, boolean online) {
        String key = String.format(USER_PRESENCE_KEY, userId);
        if (online) {
            redisTemplate.opsForValue().set(key, "online", Duration.ofMinutes(5));
        } else {
            redisTemplate.delete(key);
        }
    }

    private void addUserActiveConversation(Integer userId, Integer conversationId) {
        String key = String.format(USER_ACTIVE_CONVERSATIONS_KEY, userId);
        redisTemplate.opsForSet().add(key, conversationId.toString());
        redisTemplate.expire(key, Duration.ofHours(1));
    }

    private void removeUserActiveConversation(Integer userId, Integer conversationId) {
        String key = String.format(USER_ACTIVE_CONVERSATIONS_KEY, userId);
        redisTemplate.opsForSet().remove(key, conversationId.toString());
    }

    private void addConversationActiveUser(Integer conversationId, Integer userId) {
        String key = String.format(CONVERSATION_ACTIVE_USERS_KEY, conversationId);
        redisTemplate.opsForSet().add(key, userId.toString());
        redisTemplate.expire(key, Duration.ofHours(1));
    }

    private void removeConversationActiveUser(Integer conversationId, Integer userId) {
        String key = String.format(CONVERSATION_ACTIVE_USERS_KEY, conversationId);
        redisTemplate.opsForSet().remove(key, userId.toString());
    }

    private void cleanupUserPresence(Integer userId) {
        // Get all conversations user was active in
        String userConversationsKey = String.format(USER_ACTIVE_CONVERSATIONS_KEY, userId);
        Set<Object> conversationIds = redisTemplate.opsForSet().members(userConversationsKey);
        
        if (conversationIds != null) {
            for (Object convIdObj : conversationIds) {
                try {
                    Integer conversationId = Integer.parseInt(convIdObj.toString());
                    removeConversationActiveUser(conversationId, userId);
                    chatRealtimeService.broadcastUserPresence(conversationId, userId, false);
                } catch (NumberFormatException e) {
                    log.warn("Invalid conversation ID in cleanup: {}", convIdObj);
                }
            }
        }
        
        // Clean up user's data
        redisTemplate.delete(userConversationsKey);
        redisTemplate.delete(String.format(USER_SESSIONS_KEY, userId));
        redisTemplate.delete(String.format(USER_LAST_ACTIVITY_KEY, userId));
        
        log.info("Cleaned up presence data for user {}", userId);
    }

    private Integer extractUserIdFromHeaders(StompHeaderAccessor headers) {
        try {
            Authentication userAuth = (Authentication) headers.getUser();
            if (userAuth != null && userAuth.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) userAuth.getPrincipal();
                return userDetails.getId();
            }
        } catch (Exception e) {
            log.warn("Failed to extract user ID from WebSocket headers", e);
        }
        return null;
    }

    // ========== Public Utility Methods ==========

    public boolean isUserOnline(Integer userId) {
        String key = String.format(USER_PRESENCE_KEY, userId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    public Set<Integer> getUserActiveConversations(Integer userId) {
        String key = String.format(USER_ACTIVE_CONVERSATIONS_KEY, userId);
        Set<Object> conversationIds = redisTemplate.opsForSet().members(key);
        Set<Integer> result = new HashSet<>();
        
        if (conversationIds != null) {
            for (Object convId : conversationIds) {
                try {
                    result.add(Integer.parseInt(convId.toString()));
                } catch (NumberFormatException e) {
                    log.warn("Invalid conversation ID: {}", convId);
                }
            }
        }
        
        return result;
    }
    
    public List<Integer> getActiveUsersInConversation(Integer conversationId) {
        String key = String.format(CONVERSATION_ACTIVE_USERS_KEY, conversationId);
        List<Integer> activeUserIds = new ArrayList<>();
        ScanOptions scanOptions = ScanOptions.scanOptions().match("*").count(1000).build();

        try (Cursor<Object> cursor = redisTemplate.opsForSet().scan(key, scanOptions)) {
            while (cursor.hasNext()) {
                Object userIdObj = cursor.next();
                if (userIdObj != null) {
                    try {
                        activeUserIds.add(Integer.parseInt(userIdObj.toString()));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid user ID found in Redis set {} for conversation {}: {}", key, conversationId, userIdObj);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error scanning Redis set {} for conversation {}: {}", key, conversationId, e.getMessage(), e);
            return Collections.emptyList();
        }

        log.debug("Found {} active users in conversation {} using SSCAN", activeUserIds.size(), conversationId);
        return activeUserIds;
    }

    public int getActiveUsersCountInConversation(Integer conversationId) {
        String key = String.format(CONVERSATION_ACTIVE_USERS_KEY, conversationId);
        Long count = redisTemplate.opsForSet().size(key);
        return count != null ? count.intValue() : 0;
    }

    public Instant getUserLastActivity(Integer userId) {
        String key = String.format(USER_LAST_ACTIVITY_KEY, userId);
        String lastActivityStr = (String) redisTemplate.opsForValue().get(key);
        
        if (lastActivityStr != null) {
            try {
                return Instant.parse(lastActivityStr);
            } catch (Exception e) {
                log.warn("Failed to parse last activity for user {}: {}", userId, e.getMessage());
            }
        }
        
        return null;
    }
}