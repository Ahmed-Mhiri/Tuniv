package com.tuniv.backend.chat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping; // <-- IMPORT ADDED
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.service.ChatService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat.sendMessage/{conversationId}")
    public void sendMessage(@DestinationVariable Integer conversationId, 
                              @Payload ChatMessageDto chatMessageDto,
                              Authentication authentication) {
        String senderUsername = authentication.getName();
        chatService.sendMessage(conversationId, chatMessageDto, senderUsername);
    }
    
    // --- METHOD UPDATED ---
    @GetMapping("/api/v1/conversations/{conversationId}/messages")
    @ResponseBody
    public ResponseEntity<List<ChatMessageDto>> getMessageHistory(@PathVariable Integer conversationId) {
        List<ChatMessageDto> messages = chatService.getMessagesByConversation(conversationId);
        return ResponseEntity.ok(messages);
    }
}