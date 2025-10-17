package com.tuniv.backend.chat.controller;

import java.security.Principal;
import java.util.stream.Collectors;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.tuniv.backend.authorization.service.PermissionService;
import com.tuniv.backend.chat.annotation.RequiresMembership;
import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.dto.ChatNotificationDto;
import com.tuniv.backend.chat.dto.EditMessageRequest;
import com.tuniv.backend.chat.dto.ReactionRequestDto;
import com.tuniv.backend.chat.dto.ReadReceiptDto;
import com.tuniv.backend.chat.dto.RemoveReactionRequestDto;
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

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
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

    // ========== Message Operations ==========

    @MessageMapping("/chat/{conversationId}/send")
    @SendToUser("/queue/messages/ack")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ChatMessageDto sendMessage(
            @DestinationVariable Integer conversationId,
            @Payload SendMessageRequest request,
            Principal principal) {
        
        UserDetailsImpl currentUser = extractUserDetails(principal);
        
        log.info("WebSocket: Sending message to conversation {} by user {}", conversationId, currentUser.getId());
        
        // Membership validation is handled by AOP - @RequiresMembership annotation
        // Specific permission check for sending messages
        if (!permissionService.hasPermission(currentUser.getId(), "chat.message.send", conversationId)) {
            throw new AccessDeniedException("No permission to send messages in this conversation");
        }
        
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
        
        // Message-level permissions are handled by MessageService
        return messageService.editMessage(messageId, request, currentUser);
    }

    @MessageMapping("/chat/messages/{messageId}/delete")
    public void deleteMessage(
            @DestinationVariable Integer messageId,
            Principal principal) {
        
        UserDetailsImpl currentUser = extractUserDetails(principal);
        
        log.info("WebSocket: Deleting message {} by user {}", messageId, currentUser.getId());
        
        // Message-level permissions are handled by MessageService
        messageService.deleteMessage(messageId, currentUser);
    }

    // ========== Reaction Operations ==========

    @MessageMapping("/chat/messages/{messageId}/reactions/add")
    @RequiresMembership // Membership will be inferred from message context
    public void addReaction(
            @DestinationVariable Integer messageId,
            @Payload ReactionRequestDto request,
            Principal principal) {
        
        UserDetailsImpl currentUser = extractUserDetails(principal);
        
        log.info("WebSocket: Adding reaction to message {} by user {}", messageId, currentUser.getId());
        
        // Membership validation handled by AOP, message permissions by service
        messageService.addOrUpdateReaction(messageId, request, currentUser);
    }

    @MessageMapping("/chat/messages/{messageId}/reactions/remove")
    @RequiresMembership // Membership will be inferred from message context
    public void removeReaction(
            @DestinationVariable Integer messageId,
            @Payload @Valid RemoveReactionRequestDto request,
            Principal principal) {
        
        UserDetailsImpl currentUser = extractUserDetails(principal);
        
        log.info("WebSocket: Removing reaction from message {} by user {}", messageId, currentUser.getId());
        
        // Membership validation handled by AOP, message permissions by service
        messageService.removeReaction(messageId, request.getEmoji(), currentUser);
    }

    // ========== Typing Indicators ==========

    @MessageMapping("/chat/{conversationId}/typing")
    @RequiresMembership(conversationIdParam = "conversationId")
    public void handleTyping(
            @DestinationVariable Integer conversationId,
            @Payload TypingIndicatorDto typingIndicator,
            Principal principal) {
        
        UserDetailsImpl currentUser = extractUserDetails(principal);
        
        log.debug("WebSocket: User {} typing in conversation {}", currentUser.getId(), conversationId);
        
        // Membership validation handled by AOP - @RequiresMembership annotation
        
        typingIndicator.setUserId(currentUser.getId());
        typingIndicator.setUsername(currentUser.getUsername());
        typingIndicator.setConversationId(conversationId);
        
        chatRealtimeService.broadcastTypingIndicator(conversationId, typingIndicator);
    }

    @MessageMapping("/chat/{conversationId}/stop-typing")
    @RequiresMembership(conversationIdParam = "conversationId")
    public void handleStopTyping(
            @DestinationVariable Integer conversationId,
            Principal principal) {
        
        UserDetailsImpl currentUser = extractUserDetails(principal);
        
        log.debug("WebSocket: User {} stopped typing in conversation {}", currentUser.getId(), conversationId);
        
        // Membership validation handled by AOP - @RequiresMembership annotation
        
        chatRealtimeService.broadcastStopTyping(conversationId, currentUser.getId());
    }

    // ========== Presence & Read Receipts ==========

    @MessageMapping("/chat/{conversationId}/presence")
    @RequiresMembership(conversationIdParam = "conversationId")
    public void handlePresence(
            @DestinationVariable Integer conversationId,
            @Payload UserPresenceDto presence,
            Principal principal) {
        
        UserDetailsImpl currentUser = extractUserDetails(principal);
        
        log.debug("WebSocket: User {} presence update in conversation {}: online={}", 
                 currentUser.getId(), conversationId, presence.isOnline());
        
        // Membership validation handled by AOP - @RequiresMembership annotation
        
        presence.setUserId(currentUser.getId());
        presence.setUsername(currentUser.getUsername());
        presence.setConversationId(conversationId);
        
        chatRealtimeService.broadcastUserPresence(conversationId, currentUser.getId(), presence.isOnline());
    }

    @MessageMapping("/chat/{conversationId}/read-receipt")
    @RequiresMembership(conversationIdParam = "conversationId")
    public void handleReadReceipt(
            @DestinationVariable Integer conversationId,
            @Payload ReadReceiptDto readReceipt,
            Principal principal) {
        
        UserDetailsImpl currentUser = extractUserDetails(principal);
        
        log.debug("WebSocket: User {} read receipt in conversation {}", currentUser.getId(), conversationId);
        
        // Membership validation handled by AOP - @RequiresMembership annotation
        
        readReceipt.setUserId(currentUser.getId());
        readReceipt.setUsername(currentUser.getUsername());
        readReceipt.setConversationId(conversationId);
        
        chatRealtimeService.broadcastReadReceipt(conversationId, readReceipt);
    }

    // ========== Subscription Handlers ==========

    @SubscribeMapping("/user/queue/notifications")
    public void handleUserSubscription(SimpMessageHeaderAccessor headerAccessor) {
        Authentication auth = (Authentication) headerAccessor.getUser();
        if (auth != null) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            log.debug("WebSocket: User {} subscribed to notifications", userDetails.getId());
            
            sendWelcomeNotification(userDetails);
        }
    }

    @SubscribeMapping("/topic/conversations/{conversationId}")
    @RequiresMembership(conversationIdParam = "conversationId")
    public void handleConversationSubscription(
            @DestinationVariable Integer conversationId,
            SimpMessageHeaderAccessor headerAccessor) {
        
        Authentication auth = (Authentication) headerAccessor.getUser();
        if (auth != null) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            
            log.debug("WebSocket: User {} subscribed to conversation {}", userDetails.getId(), conversationId);
            
            // Membership validation handled by AOP - @RequiresMembership annotation
            
            chatRealtimeService.broadcastUserActiveStatus(conversationId, userDetails.getId(), true);
            
            sendConversationState(conversationId, userDetails);
        }
    }

    @SubscribeMapping("/user/queue/messages/ack")
    public void handleMessageAckSubscription(SimpMessageHeaderAccessor headerAccessor) {
        Authentication auth = (Authentication) headerAccessor.getUser();
        if (auth != null) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            log.debug("WebSocket: User {} subscribed to message acknowledgments", userDetails.getId());
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

    // ========== Exception Handlers ==========

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

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser("/queue/errors")
    public ErrorMessage handleValidationException(MethodArgumentNotValidException exception, Principal principal) {
        log.warn("Validation error for user {}: {}", principal.getName(), exception.getMessage());
    
        String errorMessage = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        ErrorMessage error = new ErrorMessage();
        error.setType("VALIDATION_ERROR");
        error.setCode("INVALID_INPUT");
        error.setMessage(errorMessage);
        error.setTimestamp(java.time.Instant.now());
        
        return error;
    }

    @MessageExceptionHandler(ConstraintViolationException.class)
    @SendToUser("/queue/errors")
    public ErrorMessage handleConstraintViolationException(ConstraintViolationException exception, Principal principal) {
        log.warn("Constraint violation for user {}: {}", principal.getName(), exception.getMessage());
        
        String errorMessage = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        
        ErrorMessage error = new ErrorMessage();
        error.setType("VALIDATION_ERROR");
        error.setCode("INVALID_INPUT");
        error.setMessage(errorMessage);
        error.setTimestamp(java.time.Instant.now());
        
        return error;
    }

    // ========== Private Helper Methods ==========

    private UserDetailsImpl extractUserDetails(Principal principal) {
        if (principal instanceof Authentication) {
            Authentication auth = (Authentication) principal;
            if (auth.getPrincipal() instanceof UserDetailsImpl) {
                return (UserDetailsImpl) auth.getPrincipal();
            }
        }
        throw new SecurityException("Unable to extract user details from principal");
    }

    private User getUserEntity(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    /**
     * Note: This method is no longer needed for WebSocket operations
     * as membership validation is now handled by the AOP aspect
     */
    @Deprecated
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
            var activeUsers = chatRealtimeService.getActiveUsersInConversation(conversationId);
            
            log.debug("Sent conversation state to user {} for conversation {}", userDetails.getId(), conversationId);
        } catch (Exception e) {
            log.warn("Failed to send conversation state to user {}: {}", userDetails.getId(), e.getMessage());
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