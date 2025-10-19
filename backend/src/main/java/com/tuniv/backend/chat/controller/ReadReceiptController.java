package com.tuniv.backend.chat.controller;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.dto.ReadReceiptDto;
import com.tuniv.backend.chat.dto.UnreadCountDto;
import com.tuniv.backend.chat.service.ReadReceiptService;
import com.tuniv.backend.chat.annotation.RequiresMembership;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ReadReceiptController {

    private final ReadReceiptService readReceiptService;

    @PostMapping("/messages/{messageId}/read")
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<ApiResponse> markMessageAsRead(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.debug("Marking message {} as read by user {}", messageId, currentUser.getId());
        readReceiptService.markMessageAsRead(messageId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Message marked as read"));
    }

    @PostMapping("/conversations/{conversationId}/read-all")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<ApiResponse> markAllMessagesAsRead(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.debug("Marking all messages as read in conversation {} by user {}", conversationId, currentUser.getId());
        readReceiptService.markAllMessagesAsRead(conversationId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("All messages marked as read"));
    }

    @GetMapping("/conversations/{conversationId}/unread-count")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<UnreadCountDto> getUnreadCount(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        UnreadCountDto unreadCount = readReceiptService.getUnreadCount(conversationId, currentUser);
        return ResponseEntity.ok(unreadCount);
    }

    @GetMapping("/conversations/{conversationId}/unread-messages")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<Page<ChatMessageDto>> getUnreadMessages(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PageableDefault(size = 50) Pageable pageable) {
        log.debug("Fetching unread messages for conversation {} by user {}", conversationId, currentUser.getId());
        Page<ChatMessageDto> messages = readReceiptService.getUnreadMessages(conversationId, currentUser, pageable);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/messages/{messageId}/read-receipts")
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<Page<ReadReceiptDto>> getMessageReadReceipts(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching read receipts for message {} by user {}", messageId, currentUser.getId());
        Page<ReadReceiptDto> receipts = readReceiptService.getMessageReadReceipts(messageId, currentUser, pageable);
        return ResponseEntity.ok(receipts);
    }
}