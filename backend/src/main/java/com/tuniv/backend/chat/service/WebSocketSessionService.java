package com.tuniv.backend.chat.service;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketSessionService {

    private final ChatRealtimeService chatRealtimeService;
    private final RedisTemplate<String, Object> redisTemplate;

    // Track active sessions in memory (ephemeral)
    private final Map<String, Integer> sessionToUserMap = new ConcurrentHashMap<>();
    
    // Redis key patterns
    private static final String USER_SESSIONS_KEY = "user:sessions:%d";
    private static final String USER_ACTIVE_CONVERSATIONS_KEY = "user:active:conversations:%d";
    private static final String CONVERSATION_ACTIVE_USERS_KEY = "conversation:active:users:%d";
    private static final String USER_PRESENCE_KEY = "user:presence:%d";

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();
        
        Integer userId = extractUserIdFromHeaders(headers);
        
        if (userId != null) {
            // Store session mapping in memory
            sessionToUserMap.put(sessionId, userId);
            
            // Store in Valkey for distributed access
            addUserSession(userId, sessionId);
            
            // Set user as online
            setUserOnline(userId, true);
            
            log.debug("User {} connected with session {}", userId, sessionId);
        } else {
            log.warn("WebSocket connection without user authentication: {}", sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();
        
        Integer userId = sessionToUserMap.remove(sessionId);
        if (userId != null) {
            // Remove session from Valkey
            removeUserSession(userId, sessionId);
            
            // Check if user has any remaining sessions
            if (getUserSessionCount(userId) == 0) {
                // User has no more active sessions - set offline
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
        
        Integer userId = sessionToUserMap.get(sessionId);
        if (userId != null && destination != null && destination.startsWith("/topic/conversations/")) {
            try {
                String[] parts = destination.split("/");
                Integer conversationId = Integer.parseInt(parts[parts.length - 1]);
                
                // Mark user as active in this conversation in Valkey
                addUserActiveConversation(userId, conversationId);
                addConversationActiveUser(conversationId, userId);
                
                // Broadcast presence update
                chatRealtimeService.broadcastUserPresence(conversationId, userId, true);
                
                log.debug("User {} subscribed to conversation {}", userId, conversationId);
            } catch (NumberFormatException e) {
                log.warn("Invalid conversation ID in destination: {}", destination);
            }
        }
    }

    // Valkey Operations
    private void addUserSession(Integer userId, String sessionId) {
        String key = String.format(USER_SESSIONS_KEY, userId);
        redisTemplate.opsForSet().add(key, sessionId);
        redisTemplate.expire(key, Duration.ofHours(24)); // Session timeout
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

    private void setUserOnline(Integer userId, boolean online) {
        String key = String.format(USER_PRESENCE_KEY, userId);
        if (online) {
            redisTemplate.opsForValue().set(key, "online", Duration.ofMinutes(5)); // Refresh every 5 min
        } else {
            redisTemplate.delete(key);
        }
    }

    private void addUserActiveConversation(Integer userId, Integer conversationId) {
        String key = String.format(USER_ACTIVE_CONVERSATIONS_KEY, userId);
        redisTemplate.opsForSet().add(key, conversationId.toString());
        redisTemplate.expire(key, Duration.ofHours(1)); // Refresh on activity
    }

    private void addConversationActiveUser(Integer conversationId, Integer userId) {
        String key = String.format(CONVERSATION_ACTIVE_USERS_KEY, conversationId);
        redisTemplate.opsForSet().add(key, userId.toString());
        redisTemplate.expire(key, Duration.ofHours(1)); // Refresh on activity
    }

    private void cleanupUserPresence(Integer userId) {
        // Get all conversations user was active in
        String userConversationsKey = String.format(USER_ACTIVE_CONVERSATIONS_KEY, userId);
        Set<Object> conversationIds = redisTemplate.opsForSet().members(userConversationsKey);
        
        if (conversationIds != null) {
            for (Object convIdObj : conversationIds) {
                try {
                    Integer conversationId = Integer.parseInt(convIdObj.toString());
                    
                    // Remove user from conversation's active users
                    String convUsersKey = String.format(CONVERSATION_ACTIVE_USERS_KEY, conversationId);
                    redisTemplate.opsForSet().remove(convUsersKey, userId.toString());
                    
                    // Broadcast user offline status
                    chatRealtimeService.broadcastUserPresence(conversationId, userId, false);
                } catch (NumberFormatException e) {
                    log.warn("Invalid conversation ID in cleanup: {}", convIdObj);
                }
            }
        }
        
        // Clean up user's data
        redisTemplate.delete(userConversationsKey);
        redisTemplate.delete(String.format(USER_SESSIONS_KEY, userId));
        
        log.info("Cleaned up Valkey presence for user {}", userId);
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

    // Updated utility methods using Valkey
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
        Set<Object> userIds = redisTemplate.opsForSet().members(key);
        List<Integer> result = new ArrayList<>();
        
        if (userIds != null) {
            for (Object userId : userIds) {
                try {
                    result.add(Integer.parseInt(userId.toString()));
                } catch (NumberFormatException e) {
                    log.warn("Invalid user ID: {}", userId);
                }
            }
        }
        
        return result;
    }
}