package com.tuniv.backend.chat.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import com.tuniv.backend.authorization.service.PermissionService;
import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.dto.ChatNotificationDto;
import com.tuniv.backend.chat.dto.EditMessageRequest;
import com.tuniv.backend.chat.dto.ReactionRequestDto;
import com.tuniv.backend.chat.dto.ReadReceiptDto;
import com.tuniv.backend.chat.dto.SendMessageRequest;
import com.tuniv.backend.chat.dto.TypingIndicatorDto;
import com.tuniv.backend.chat.dto.UserPresenceDto;
import com.tuniv.backend.chat.service.ChatRealtimeService;
import com.tuniv.backend.chat.service.ConversationService;
import com.tuniv.backend.chat.service.MessageService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatRealtimeService chatRealtimeService;
    private final MessageService messageService;
    private final ConversationService conversationService;
    private final PermissionService permissionService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ========== Message Events ==========

    @MessageMapping("/chat/{conversationId}/typing")
    public void handleTyping(
            @DestinationVariable Integer conversationId,
            @Payload TypingIndicatorDto typingIndicator,
            Principal principal) {
        
        UserDetailsImpl currentUser = extractUserDetails(principal);
        
        log.debug("WebSocket: User {} typing in conversation {}", currentUser.getId(), conversationId);
        
        // ✅ MODIFIED: Pass userId directly instead of User object
        validateConversationMembership(conversationId, currentUser.getId());
        
        // Set the user ID from the authenticated user (security measure)
        typingIndicator.setUserId(currentUser.getId());
        typingIndicator.setUsername(currentUser.getUsername());
        
        chatRealtimeService.broadcastTypingIndicator(conversationId, typingIndicator);
    }

    @MessageMapping("/chat/{conversationId}/stop-typing")
    public void handleStopTyping(
            @DestinationVariable Integer conversationId,
            Principal principal) {
        
        UserDetailsImpl currentUser = extractUserDetails(principal);
        
        log.debug("WebSocket: User {} stopped typing in conversation {}", currentUser.getId(), conversationId);
        
        // ✅ MODIFIED: Pass userId directly
        validateConversationMembership(conversationId, currentUser.getId());
        
        chatRealtimeService.broadcastStopTyping(conversationId, currentUser.getId());
    }

    // ========== Message Management Events ==========

    @MessageMapping("/chat/messages/{messageId}/delete")
    public void deleteMessage(
            @DestinationVariable Integer messageId,
            Principal principal) {
        
        UserDetailsImpl currentUser = extractUserDetails(principal);
        
        log.info("WebSocket: Deleting message {} by user {}", messageId, currentUser.getId());
        
        // Permission check is handled in MessageService
        messageService.deleteMessage(messageId, currentUser);
    }

    // ========== Reaction Events ==========

    @MessageMapping("/chat/messages/{messageId}/reactions/add")
    public void addReaction(
            @DestinationVariable Integer messageId,
            @Payload ReactionRequestDto request,
            Principal principal) {
        
        UserDetailsImpl currentUser = extractUserDetails(principal);
        
        log.info("WebSocket: Adding reaction to message {} by user {}", messageId, currentUser.getId());
        
        // Permission check is handled in MessageService
        messageService.addOrUpdateReaction(messageId, request, currentUser);
    }

    @MessageMapping("/chat/messages/{messageId}/reactions/remove")
    public void removeReaction(
            @DestinationVariable Integer messageId,
            @RequestParam String emoji,
            Principal principal) {
        
        UserDetailsImpl currentUser = extractUserDetails(principal);
        
        log.info("WebSocket: Removing reaction from message {} by user {}", messageId, currentUser.getId());
        
        messageService.removeReaction(messageId, emoji, currentUser);
    }

    // ========== Presence & Activity Events ==========

    @MessageMapping("/chat/{conversationId}/presence")
    public void handlePresence(
            @DestinationVariable Integer conversationId,
            @Payload UserPresenceDto presence,
            Principal principal) {
        
        UserDetailsImpl currentUser = extractUserDetails(principal);
        
        log.debug("WebSocket: User {} presence update in conversation {}: online={}", 
                 currentUser.getId(), conversationId, presence.isOnline());
        
        // ✅ MODIFIED: Pass userId directly
        validateConversationMembership(conversationId, currentUser.getId());
        
        // Ensure the presence data matches the authenticated user
        presence.setUserId(currentUser.getId());
        presence.setUsername(currentUser.getUsername());
        
        chatRealtimeService.broadcastUserPresence(conversationId, currentUser.getId(), presence.isOnline());
    }

    @MessageMapping("/chat/{conversationId}/read-receipt")
    public void handleReadReceipt(
            @DestinationVariable Integer conversationId,
            @Payload ReadReceiptDto readReceipt,
            Principal principal) {
        
        UserDetailsImpl currentUser = extractUserDetails(principal);
        
        log.debug("WebSocket: User {} read receipt in conversation {}", currentUser.getId(), conversationId);
        
        // ✅ MODIFIED: Pass userId directly
        validateConversationMembership(conversationId, currentUser.getId());
        
        // Set user info from authentication
        readReceipt.setUserId(currentUser.getId());
        readReceipt.setUsername(currentUser.getUsername());
        
        chatRealtimeService.broadcastReadReceipt(conversationId, readReceipt);
    }

    // ========== Subscription Handlers ==========

    @SubscribeMapping("/user/queue/notifications")
    public void handleUserSubscription(SimpMessageHeaderAccessor headerAccessor) {
        Authentication auth = (Authentication) headerAccessor.getUser();
        if (auth != null) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            log.debug("WebSocket: User {} subscribed to notifications", userDetails.getId());
            
            // Send welcome message or initial notifications
            sendWelcomeNotification(userDetails);
        }
    }

    @SubscribeMapping("/topic/conversations/{conversationId}")
    public void handleConversationSubscription(
            @DestinationVariable Integer conversationId,
            SimpMessageHeaderAccessor headerAccessor) {
        
        Authentication auth = (Authentication) headerAccessor.getUser();
        if (auth != null) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            
            log.debug("WebSocket: User {} subscribed to conversation {}", userDetails.getId(), conversationId);
            
            // ✅ MODIFIED: Pass userId directly
            validateConversationMembership(conversationId, userDetails.getId());
            
            // Notify others that user is now active in this conversation
            chatRealtimeService.broadcastUserActiveStatus(conversationId, userDetails.getId(), true);
            
            // Send conversation state (pinned messages, active users, etc.)
            sendConversationState(conversationId, userDetails);
        }
    }

    @SubscribeMapping("/user/queue/errors")
    public void handleErrorSubscription(SimpMessageHeaderAccessor headerAccessor) {
        Authentication auth = (Authentication) headerAccessor.getUser();
        if (auth != null) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            log.debug("WebSocket: User {} subscribed to error channel", userDetails.getId());
        }
    }

    // ========== Error Handling ==========

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public ErrorMessage handleException(Exception exception, Principal principal) {
        log.error("WebSocket error for user {}: {}", principal.getName(), exception.getMessage());
        
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setType("ERROR");
        errorMessage.setMessage(exception.getMessage());
        errorMessage.setTimestamp(java.time.Instant.now());
        
        if (exception instanceof AccessDeniedException) {
            errorMessage.setCode("ACCESS_DENIED");
        } else if (exception instanceof ResourceNotFoundException) {
            errorMessage.setCode("NOT_FOUND");
        } else {
            errorMessage.setCode("INTERNAL_ERROR");
        }
        
        return errorMessage;
    }

    // ========== Utility Methods ==========

    private UserDetailsImpl extractUserDetails(Principal principal) {
        if (principal instanceof Authentication) {
            Authentication auth = (Authentication) principal;
            return (UserDetailsImpl) auth.getPrincipal();
        }
        throw new SecurityException("Unable to extract user details from principal");
    }

    private User getUserEntity(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    // ✅ MODIFIED: Updated to accept userId instead of User object
    private void validateConversationMembership(Integer conversationId, Integer userId) {
        if (!conversationService.hasConversationPermission(userId, conversationId, "chat.conversation.view")) {
            throw new AccessDeniedException("Not a member of this conversation");
        }
    }

    private void sendWelcomeNotification(UserDetailsImpl userDetails) {
        ChatNotificationDto welcomeNotification = new ChatNotificationDto();
        welcomeNotification.setType("WELCOME");
        welcomeNotification.setTitle("Connected");
        welcomeNotification.setMessage("You are now connected to the chat service");
        welcomeNotification.setTimestamp(java.time.Instant.now());
        
        chatRealtimeService.sendDirectNotification(userDetails.getId(), welcomeNotification);
    }

    private void sendConversationState(Integer conversationId, UserDetailsImpl userDetails) {
        try {
            // Send active users list
            var activeUsers = chatRealtimeService.getActiveUsersInConversation(conversationId);
            
            // Send typing indicators
            // This would require storing typing state in ChatRealtimeService
            
            log.debug("Sent conversation state to user {} for conversation {}", userDetails.getId(), conversationId);
        } catch (Exception e) {
            log.warn("Failed to send conversation state to user {}: {}", userDetails.getId(), e.getMessage());
        }
    }

    @MessageMapping("/chat/{conversationId}/send")
    @SendToUser("/queue/messages/ack")
    public ChatMessageDto sendMessage(
            @DestinationVariable Integer conversationId,
            @Payload SendMessageRequest request,
            Principal principal) {
        
        UserDetailsImpl currentUser = extractUserDetails(principal);
        
        log.info("WebSocket: Sending message to conversation {} by user {}", conversationId, currentUser.getId());
        
        // ✅ MODIFIED: Pass userId directly
        validateConversationMembership(conversationId, currentUser.getId());
        
        // Check if user has permission to send messages in this conversation
        if (!permissionService.hasPermission(currentUser.getId(), "chat.message.send", conversationId)) {
            throw new AccessDeniedException("No permission to send messages in this conversation");
        }
        
        // The service method broadcasts to all participants and returns the DTO for acknowledgment
        return messageService.sendMessage(conversationId, request, currentUser);
    }

    @MessageMapping("/chat/messages/{messageId}/edit")
    @SendToUser("/queue/messages/ack")
    public ChatMessageDto editMessage(
            @DestinationVariable Integer messageId,
            @Payload EditMessageRequest request,
            Principal principal) {
        
        UserDetailsImpl currentUser = extractUserDetails(principal);
        
        log.info("WebSocket: Editing message {} by user {}", messageId, currentUser.getId());
        
        // Permission check is handled in MessageService
        return messageService.editMessage(messageId, request, currentUser);
    }

    // Add subscription handler for acknowledgments
    @SubscribeMapping("/user/queue/messages/ack")
    public void handleMessageAckSubscription(SimpMessageHeaderAccessor headerAccessor) {
        Authentication auth = (Authentication) headerAccessor.getUser();
        if (auth != null) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            log.debug("WebSocket: User {} subscribed to message acknowledgments", userDetails.getId());
        }
    }

    // ========== Inner Classes ==========

    public static class ErrorMessage {
        private String type;
        private String code;
        private String message;
        private java.time.Instant timestamp;

        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public java.time.Instant getTimestamp() { return timestamp; }
        public void setTimestamp(java.time.Instant timestamp) { this.timestamp = timestamp; }
    }
}