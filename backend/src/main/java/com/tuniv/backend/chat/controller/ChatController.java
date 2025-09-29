package com.tuniv.backend.chat.controller;

import java.security.Principal;
import java.util.Collections;
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
    private final ChatMapper chatMapper; // Injected for use in the REST endpoint

    /**
     * WebSocket endpoint for sending a message. This is typically used for text-only messages
     * sent from an active WebSocket session. The service handles broadcasting the message.
     *
     * @param conversationId The ID of the conversation to send the message to.
     * @param chatMessageDto The message payload.
     * @param principal      The currently authenticated user.
     */
    @MessageMapping("/chat/{conversationId}/sendMessage")
    public void sendMessage(
            @DestinationVariable Integer conversationId,
            @Payload ChatMessageDto chatMessageDto,
            Principal principal
    ) {
        // Files are null here as this is a text-only WebSocket endpoint
        chatService.sendMessage(conversationId, chatMessageDto, principal.getName(), null);
    }

    /**
     * REST endpoint for sending a message, with optional file attachments.
     * This is ideal for form submissions that include both text and files.
     *
     * @param conversationId The ID of the conversation.
     * @param chatMessageDto The DTO containing message content.
     * @param files          A list of optional file attachments.
     * @return A ResponseEntity containing the DTO of the saved message.
     */
    @PostMapping(value = "/api/v1/chat/{conversationId}/message", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ChatMessageDto> sendMessageWithAttachment(
            @PathVariable Integer conversationId,
            @RequestPart("message") @Valid ChatMessageDto chatMessageDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Message savedMessage = chatService.sendMessage(conversationId, chatMessageDto, currentUser.getUsername(), files);

        // Map the saved entity back to a DTO for the HTTP response.
        // This confirms to the client what was saved and includes the server-generated ID and timestamp.
        ChatMessageDto responseDto = chatMapper.toChatMessageDto(
                savedMessage,
                Collections.emptyList(), // A new message has no reactions yet
                currentUser.getUsername(),
                chatMessageDto.getClientTempId() // Include the temporary client ID for reconciliation
        );

        return ResponseEntity.ok(responseDto);
    }

    /**
     * REST endpoint to fetch the message history for a specific conversation.
     *
     * @param conversationId The ID of the conversation.
     * @return A list of message DTOs.
     */
    @GetMapping("/api/v1/chat/{conversationId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessageHistory(@PathVariable Integer conversationId) {
        List<ChatMessageDto> messages = chatService.getMessagesByConversation(conversationId);
        return ResponseEntity.ok(messages);
    }

    /**
     * REST endpoint to fetch a single message by its ID.
     *
     * @param messageId The ID of the message.
     * @return The message DTO.
     */
    @GetMapping("/api/v1/messages/{messageId}")
    public ResponseEntity<ChatMessageDto> getSingleMessage(@PathVariable Integer messageId) {
        ChatMessageDto message = chatService.getSingleMessageById(messageId);
        return ResponseEntity.ok(message);
    }

    /**
     * REST endpoint to delete a message. The service layer handles authorization.
     *
     * @param messageId The ID of the message to delete.
     * @return A ResponseEntity with no content.
     */
    @DeleteMapping("/api/v1/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Integer messageId) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        chatService.deleteMessage(messageId, currentUser.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * REST endpoint to add or remove a reaction from a message.
     *
     * @param messageId       The ID of the message to react to.
     * @param reactionRequest DTO containing the emoji for the reaction.
     * @return A ResponseEntity indicating success.
     */
    @PostMapping("/api/v1/messages/{messageId}/reactions")
    public ResponseEntity<Void> toggleReaction(
            @PathVariable Integer messageId,
            @RequestBody @Valid ReactionRequestDto reactionRequest
    ) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        chatService.toggleReaction(messageId, reactionRequest.getEmoji(), currentUser.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * REST endpoint to get all conversation summaries for the currently authenticated user.
     *
     * @return A list of conversation summaries.
     */
    @GetMapping("/api/v1/chat/conversations")
    public ResponseEntity<List<ConversationSummaryDto>> getConversationsForCurrentUser() {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<ConversationSummaryDto> summaries = chatService.getConversationSummaries(currentUser.getUsername());
        return ResponseEntity.ok(summaries);
    }

    /**
     * REST endpoint to find an existing conversation with another user or create a new one.
     *
     * @param requestDto DTO containing the ID of the other participant.
     * @return The conversation summary DTO.
     */
    @PostMapping("/api/v1/chat/conversations")
    public ResponseEntity<ConversationSummaryDto> findOrCreateConversation(
            @RequestBody @Valid StartConversationRequestDto requestDto
    ) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ConversationSummaryDto summary = chatService.findOrCreateConversation(
                currentUser.getId(),
                requestDto.getParticipantId()
        );
        return ResponseEntity.ok(summary);
    }

    /**
     * REST endpoint to mark all messages in a conversation as read for the current user.
     *
     * @param conversationId The ID of the conversation to mark as read.
     * @return A ResponseEntity indicating success.
     */
    @PostMapping("/api/v1/chat/conversations/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Integer conversationId) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        chatService.markConversationAsRead(conversationId, currentUser.getUsername());
        return ResponseEntity.ok().build();
    }
}