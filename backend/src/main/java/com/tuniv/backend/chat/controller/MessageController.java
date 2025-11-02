package com.tuniv.backend.chat.controller;

import com.tuniv.backend.chat.dto.*;
import com.tuniv.backend.chat.dto.request.EditMessageRequest;
import com.tuniv.backend.chat.dto.request.SendMessageRequest;
import com.tuniv.backend.chat.dto.response.ChatMessageDto;
import com.tuniv.backend.chat.dto.response.MessageStatsDto;
import com.tuniv.backend.chat.service.MessageService;
import com.tuniv.backend.chat.annotation.RequiresMembership;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/conversation/{conversationId}")
    @PreAuthorize("@conversationPermissionService.hasPermission(#conversationId, #currentUser, 'send_messages')")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @PathVariable Integer conversationId,
            @RequestBody @Valid SendMessageRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Sending message to conversation {} by user {}", conversationId, currentUser.getId());
        ChatMessageDto message = messageService.sendMessage(conversationId, request, currentUser);
        return ResponseEntity.ok(message);
    }

    @PutMapping("/{messageId}")
    @PreAuthorize("@messagePermissionService.canInteract(#messageId, #currentUser, 'edit')")
    public ResponseEntity<ChatMessageDto> editMessage(
            @PathVariable Integer messageId,
            @RequestBody @Valid EditMessageRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Editing message {} by user {}", messageId, currentUser.getId());
        ChatMessageDto message = messageService.editMessage(messageId, request, currentUser);
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/{messageId}")
    @PreAuthorize("@messagePermissionService.canInteract(#messageId, #currentUser, 'delete')")
    public ResponseEntity<ApiResponse> deleteMessage(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Deleting message {} by user {}", messageId, currentUser.getId());
        messageService.deleteMessage(messageId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Message deleted successfully"));
    }

    @DeleteMapping("/{messageId}/permanent")
    @PreAuthorize("@messagePermissionService.canInteract(#messageId, #currentUser, 'delete_any_message')")
    public ResponseEntity<ApiResponse> permanentlyDeleteMessage(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Permanently deleting message {} by user {}", messageId, currentUser.getId());
        messageService.permanentlyDeleteMessage(messageId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Message permanently deleted"));
    }

    @GetMapping("/conversation/{conversationId}")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<Page<ChatMessageDto>> getMessagesForConversation(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PageableDefault(size = 50) Pageable pageable) {
        log.debug("Fetching messages for conversation {} by user {}", conversationId, currentUser.getId());
        Page<ChatMessageDto> messages = messageService.getMessagesForConversation(conversationId, currentUser, pageable);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/{messageId}")
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<ChatMessageDto> getMessage(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.debug("Fetching message {} by user {}", messageId, currentUser.getId());
        ChatMessageDto message = messageService.getMessage(messageId, currentUser);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ChatMessageDto>> searchMessages(
            @RequestParam String query,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Searching messages with query '{}' by user {}", query, currentUser.getId());
        Page<ChatMessageDto> messages = messageService.searchMessages(query, currentUser, pageable);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/conversation/{conversationId}/around/{aroundMessageId}")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<List<ChatMessageDto>> getMessagesAround(
            @PathVariable Integer conversationId,
            @PathVariable Integer aroundMessageId,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.debug("Fetching messages around {} in conversation {} by user {}", aroundMessageId, conversationId, currentUser.getId());
        List<ChatMessageDto> messages = messageService.getMessagesAround(conversationId, aroundMessageId, currentUser, limit);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/{messageId}/can-interact")
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<Boolean> canInteractWithMessage(
            @PathVariable Integer messageId,
            @RequestParam String action,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        boolean canInteract = messageService.canInteractWithMessage(messageId, currentUser, action);
        return ResponseEntity.ok(canInteract);
    }

    @GetMapping("/conversation/{conversationId}/stats")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<MessageStatsDto> getMessageStats(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        MessageStatsDto stats = messageService.getMessageStats(conversationId, currentUser);
        return ResponseEntity.ok(stats);
    }
}