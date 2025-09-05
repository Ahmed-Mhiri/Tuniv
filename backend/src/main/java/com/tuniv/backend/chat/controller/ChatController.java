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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.dto.ConversationSummaryDto;
import com.tuniv.backend.chat.dto.ReactionRequestDto;
import com.tuniv.backend.chat.dto.StartConversationRequestDto;
import com.tuniv.backend.chat.mapper.ChatMapper;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.service.ChatService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // WebSocket endpoint for sending a text-only message
    @MessageMapping("/chat/{conversationId}/sendMessage")
    public void sendMessage(
            @DestinationVariable Integer conversationId,
            @Payload ChatMessageDto chatMessageDto,
            Principal principal
    ) {
        chatService.sendMessage(conversationId, chatMessageDto, principal.getName(), null);
    }

    // REST endpoint for sending a message with optional files
    @PostMapping(value = "/api/v1/chat/{conversationId}/message", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
public ResponseEntity<ChatMessageDto> sendMessageWithAttachment(
        @PathVariable Integer conversationId,
        @RequestPart("message") @Valid ChatMessageDto chatMessageDto,
        @RequestPart(value = "files", required = false) List<MultipartFile> files
) {
    UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Message finalMessage = chatService.sendMessage(conversationId, chatMessageDto, currentUser.getUsername(), files);

    // ✅ FIX: Provide all required arguments: Message, Username, and clientTempId
    ChatMessageDto responseDto = ChatMapper.toChatMessageDto(
        finalMessage, 
        currentUser.getUsername(), // The missing String argument
        chatMessageDto.getClientTempId()
    );
    
    return ResponseEntity.ok(responseDto);
}

    // --- All other methods below are correct and do not need changes ---

    // REST endpoint for fetching message history
    @GetMapping("/api/v1/chat/{conversationId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessageHistory(@PathVariable Integer conversationId) {
        return ResponseEntity.ok(chatService.getMessagesByConversation(conversationId));
    }

    // REST endpoint for fetching a single message
    @GetMapping("/api/v1/messages/{messageId}")
    public ResponseEntity<ChatMessageDto> getSingleMessage(@PathVariable Integer messageId) {
        return ResponseEntity.ok(chatService.getSingleMessageById(messageId));
    }

    // REST endpoint for fetching all conversation summaries for the current user
    @GetMapping("/api/v1/chat/conversations")
    public ResponseEntity<List<ConversationSummaryDto>> getConversationsForCurrentUser() {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<ConversationSummaryDto> summaries = chatService.getConversationSummaries(currentUser.getUsername());
        return ResponseEntity.ok(summaries);
    }

    // REST endpoint for finding or creating a conversation
    @PostMapping("/api/v1/chat/conversations")
    public ResponseEntity<ConversationSummaryDto> findOrCreateConversation(
            @Valid @RequestBody StartConversationRequestDto requestDto
    ) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ConversationSummaryDto summary = chatService.findOrCreateConversation(
                currentUser.getId(),
                requestDto.getParticipantId()
        );
        return ResponseEntity.ok(summary);
    }

    // REST endpoint for marking a conversation as read
    @PostMapping("/api/v1/chat/conversations/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Integer conversationId) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        chatService.markConversationAsRead(conversationId, currentUser.getUsername());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/v1/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Integer messageId) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        chatService.deleteMessage(messageId, currentUser.getUsername());
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/api/v1/messages/{messageId}/reactions")
public ResponseEntity<Void> toggleReaction(
        @PathVariable Integer messageId,
        // ✅ Use the new DTO
        @RequestBody ReactionRequestDto reactionRequest
) {
    UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    // You can now add validation (e.g., @NotNull) to the DTO
    chatService.toggleReaction(messageId, reactionRequest.getEmoji(), currentUser.getUsername());
    return ResponseEntity.ok().build();
}
}