package com.tuniv.backend.chat.service;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketSessionService {

    private final ChatRealtimeService chatRealtimeService;
    // Track session -> user mapping
    private final Map<String, Integer> sessionToUserMap = new ConcurrentHashMap<>();
    
    // Track user -> sessions mapping (user can have multiple sessions)
    private final Map<Integer, Set<String>> userToSessionsMap = new ConcurrentHashMap<>();
    
    // Track which conversations each user is active in
    private final Map<Integer, Set<Integer>> userActiveConversations = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();
        
        // Extract user ID from authentication
        Integer userId = extractUserIdFromHeaders(headers);
        
        if (userId != null) {
            sessionToUserMap.put(sessionId, userId);
            userToSessionsMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
            
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
            // Remove session from user's sessions
            Set<String> userSessions = userToSessionsMap.get(userId);
            if (userSessions != null) {
                userSessions.remove(sessionId);
                if (userSessions.isEmpty()) {
                    userToSessionsMap.remove(userId);
                    // User has no more active sessions - cleanup presence
                    cleanupUserPresence(userId);
                }
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
            // Extract conversation ID from destination
            try {
                String[] parts = destination.split("/");
                Integer conversationId = Integer.parseInt(parts[parts.length - 1]);
                
                // Mark user as active in this conversation
                userActiveConversations.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                                      .add(conversationId);
                
                // Broadcast presence update
                chatRealtimeService.broadcastUserPresence(conversationId, userId, true);
                
                log.debug("User {} subscribed to conversation {}", userId, conversationId);
            } catch (NumberFormatException e) {
                log.warn("Invalid conversation ID in destination: {}", destination);
            }
        }
    }

    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();
        
        // Note: We don't remove from active conversations here since a user might have multiple subscriptions
        // We rely on the disconnect event for full cleanup
        log.debug("Session {} unsubscribed", sessionId);
    }

    private void cleanupUserPresence(Integer userId) {
        Set<Integer> activeConversations = userActiveConversations.remove(userId);
        if (activeConversations != null) {
            for (Integer conversationId : activeConversations) {
                // Broadcast user offline status to each conversation
                chatRealtimeService.broadcastUserPresence(conversationId, userId, false);
                log.debug("Cleaned up presence for user {} in conversation {}", userId, conversationId);
            }
        }
        
        log.info("Cleaned up WebSocket presence for user {}", userId);
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

    // ✅ REMOVED: extractUserIdFromUsername method is no longer needed

    // Utility methods
    public boolean isUserOnline(Integer userId) {
        return userToSessionsMap.containsKey(userId);
    }
    
    public Set<Integer> getUserActiveConversations(Integer userId) {
        return userActiveConversations.getOrDefault(userId, Set.of());
    }
    
    public Set<String> getUserSessions(Integer userId) {
        return userToSessionsMap.getOrDefault(userId, Set.of());
    }
}