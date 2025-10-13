package com.tuniv.backend.chat.controller;

import com.tuniv.backend.chat.dto.*;
import com.tuniv.backend.chat.service.MessageService;
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

    // ========== Core Message Actions ==========

    @PostMapping("/conversation/{conversationId}")
    @PreAuthorize("@conversationPermissionService.hasPermission(#conversationId, #currentUser, 'send_messages')")
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

    // ========== Message Retrieval ==========

    @GetMapping("/conversation/{conversationId}")
    @PreAuthorize("@conversationPermissionService.isMember(#conversationId, #currentUser)")
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
    @PreAuthorize("@conversationPermissionService.isMember(#conversationId, #currentUser)")
    public ResponseEntity<List<ChatMessageDto>> getMessagesAround(
            @PathVariable Integer conversationId,
            @PathVariable Integer aroundMessageId,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.debug("Fetching messages around {} in conversation {} by user {}", aroundMessageId, conversationId, currentUser.getId());
        List<ChatMessageDto> messages = messageService.getMessagesAround(conversationId, aroundMessageId, currentUser, limit);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/conversation/{conversationId}/unread")
    @PreAuthorize("@conversationPermissionService.isMember(#conversationId, #currentUser)")
    public ResponseEntity<Page<ChatMessageDto>> getUnreadMessages(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PageableDefault(size = 50) Pageable pageable) {
        log.debug("Fetching unread messages for conversation {} by user {}", conversationId, currentUser.getId());
        Page<ChatMessageDto> messages = messageService.getUnreadMessages(conversationId, currentUser, pageable);
        return ResponseEntity.ok(messages);
    }

    // ========== Message Interactions ==========

    @PostMapping("/{messageId}/read")
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<ApiResponse> markMessageAsRead(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.debug("Marking message {} as read by user {}", messageId, currentUser.getId());
        messageService.markMessagesAsRead(
            messageService.getMessage(messageId, currentUser).getConversationId(),
            List.of(messageId),
            currentUser
        );
        return ResponseEntity.ok(ApiResponse.success("Message marked as read"));
    }

    @PostMapping("/conversation/{conversationId}/read")
    @PreAuthorize("@conversationPermissionService.isMember(#conversationId, #currentUser)")
    public ResponseEntity<ApiResponse> markAllMessagesAsRead(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.debug("Marking all messages as read in conversation {} by user {}", conversationId, currentUser.getId());
        messageService.markAllMessagesAsRead(conversationId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("All messages marked as read"));
    }

    @GetMapping("/conversation/{conversationId}/unread-count")
    @PreAuthorize("@conversationPermissionService.isMember(#conversationId, #currentUser)")
    public ResponseEntity<UnreadCountDto> getUnreadCount(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        UnreadCountDto unreadCount = messageService.getUnreadCount(conversationId, currentUser);
        return ResponseEntity.ok(unreadCount);
    }

    // ========== Reactions ==========

    @PostMapping("/{messageId}/reactions")
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<ReactionDto> addOrUpdateReaction(
            @PathVariable Integer messageId,
            @RequestBody @Valid ReactionRequestDto request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Adding reaction to message {} by user {}", messageId, currentUser.getId());
        ReactionDto reaction = messageService.addOrUpdateReaction(messageId, request, currentUser);
        return ResponseEntity.ok(reaction);
    }

    @DeleteMapping("/{messageId}/reactions")
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<ApiResponse> removeReaction(
            @PathVariable Integer messageId,
            @RequestParam String emoji,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Removing reaction from message {} by user {}", messageId, currentUser.getId());
        messageService.removeReaction(messageId, emoji, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Reaction removed successfully"));
    }

    @DeleteMapping("/reactions/{reactionId}")
    @PreAuthorize("@messagePermissionService.isReactionOwner(#reactionId, #currentUser)")
    public ResponseEntity<ApiResponse> removeReactionById(
            @PathVariable Integer reactionId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Removing reaction {} by user {}", reactionId, currentUser.getId());
        messageService.removeReactionById(reactionId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Reaction removed successfully"));
    }

    @GetMapping("/{messageId}/reactions")
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<List<ReactionDto>> getMessageReactions(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<ReactionDto> reactions = messageService.getMessageReactions(messageId, currentUser);
        return ResponseEntity.ok(reactions);
    }

    @GetMapping("/{messageId}/reactions/summary")
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<MessageReactionsSummaryDto> getMessageReactionsSummary(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        MessageReactionsSummaryDto summary = messageService.getMessageReactionsSummary(messageId, currentUser);
        return ResponseEntity.ok(summary);
    }

    // ========== Pinning ==========

    @PostMapping("/{messageId}/pin")
    @PreAuthorize("@messagePermissionService.canInteract(#messageId, #currentUser, 'pin')")
    public ResponseEntity<PinnedMessageDto> pinMessage(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Pinning message {} by user {}", messageId, currentUser.getId());
        PinnedMessageDto pinnedMessage = messageService.pinMessage(messageId, currentUser);
        return ResponseEntity.ok(pinnedMessage);
    }

    @DeleteMapping("/{messageId}/pin")
    @PreAuthorize("@messagePermissionService.canInteract(#messageId, #currentUser, 'pin')")
    public ResponseEntity<ApiResponse> unpinMessage(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Unpinning message {} by user {}", messageId, currentUser.getId());
        messageService.unpinMessage(messageId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Message unpinned successfully"));
    }

    @GetMapping("/conversation/{conversationId}/pinned")
    @PreAuthorize("@conversationPermissionService.isMember(#conversationId, #currentUser)")
    public ResponseEntity<List<PinnedMessageDto>> getPinnedMessages(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<PinnedMessageDto> pinnedMessages = messageService.getPinnedMessages(conversationId, currentUser);
        return ResponseEntity.ok(pinnedMessages);
    }

    @GetMapping("/{messageId}/pinned")
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<Boolean> isMessagePinned(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        boolean isPinned = messageService.isMessagePinned(messageId, currentUser);
        return ResponseEntity.ok(isPinned);
    }

    // ========== Threads & Replies ==========

    @GetMapping("/{messageId}/replies")
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<Page<ChatMessageDto>> getMessageReplies(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ChatMessageDto> replies = messageService.getMessageReplies(messageId, currentUser, pageable);
        return ResponseEntity.ok(replies);
    }

    @GetMapping("/{messageId}/thread")
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<MessageThreadDto> getMessageThread(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        MessageThreadDto thread = messageService.getMessageThread(messageId, currentUser);
        return ResponseEntity.ok(thread);
    }

    // ========== Utility Endpoints ==========

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
    @PreAuthorize("@conversationPermissionService.isMember(#conversationId, #currentUser)")
    public ResponseEntity<MessageStatsDto> getMessageStats(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        MessageStatsDto stats = messageService.getMessageStats(conversationId, currentUser);
        return ResponseEntity.ok(stats);
    }

    // ========== Read Receipts Endpoint ==========

    @GetMapping("/{messageId}/read-receipts")
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<Page<ReadReceiptDto>> getMessageReadReceipts(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching read receipts for message {} by user {}", messageId, currentUser.getId());
        Page<ReadReceiptDto> receipts = messageService.getMessageReadReceipts(messageId, currentUser, pageable);
        return ResponseEntity.ok(receipts);
    }
}