package com.tuniv.backend.chat.controller;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;

@Controller // Note: Not @RestController for WebSocket methods
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // --- WebSocket Endpoint ---
    @MessageMapping("/chat.sendMessage/{conversationId}")
    public void sendMessage(@DestinationVariable Integer conversationId, 
                              @Payload ChatMessageDto chatMessageDto,
                              Authentication authentication) {
        // The authenticated user's details are in the 'authentication' object
        String senderUsername = authentication.getName();
        chatService.sendMessage(conversationId, chatMessageDto, senderUsername);
    }
    
    // --- REST Endpoint for fetching history ---
    @GetMapping("/api/v1/conversations/{conversationId}/messages")
    @ResponseBody // Important because the class is a @Controller, not @RestController
    public List<ChatMessageDto> getMessageHistory(@PathVariable Integer conversationId) {
        // Here you would implement the logic in ChatService to fetch and map messages
        return List.of(); // Placeholder
    }
}