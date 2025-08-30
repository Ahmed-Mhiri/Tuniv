package com.tuniv.backend.chat.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable; // <-- IMPORT ADDED
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.service.ChatService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller // Use @Controller for WebSocket endpoints, or RestController for REST
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // This is the WebSocket endpoint for sending a message
    @MessageMapping("/chat/{conversationId}/sendMessage")
    public void sendMessage(@DestinationVariable Integer conversationId,
                            @Payload ChatMessageDto chatMessageDto,
                            Principal principal) {
        // Note: Standard WebSockets don't support multipart files easily.
        // File uploads are typically handled with a separate REST endpoint first.
        chatService.sendMessage(conversationId, chatMessageDto, principal.getName(), null);
    }
    
    // --- NEW: A dedicated REST endpoint for sending messages with files ---
   @PostMapping(value = "/api/v1/chat/{conversationId}/message", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> sendMessageWithAttachment(
            @PathVariable Integer conversationId,
            @RequestPart("message") @Valid ChatMessageDto chatMessageDto,
            // We remove @AuthenticationPrincipal Principal from the parameters
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        // Get the authenticated user directly from the security context. This is more reliable.
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        // Pass the username from the UserDetailsImpl object to the service.
        chatService.sendMessage(conversationId, chatMessageDto, currentUser.getUsername(), files);
        
        return ResponseEntity.ok().build();
    }
    
    // This is the REST endpoint for fetching message history
    @GetMapping("/api/v1/chat/{conversationId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessageHistory(@PathVariable Integer conversationId) {
        return ResponseEntity.ok(chatService.getMessagesByConversation(conversationId));
    }
}