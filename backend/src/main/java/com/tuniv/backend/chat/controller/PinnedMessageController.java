package com.tuniv.backend.chat.controller;

import com.tuniv.backend.chat.service.PinnedMessageService;
import com.tuniv.backend.chat.annotation.RequiresMembership;
import com.tuniv.backend.chat.dto.response.PinnedMessageDto;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class PinnedMessageController {

    private final PinnedMessageService pinnedMessageService;

    @PostMapping("/messages/{messageId}/pin")
    @PreAuthorize("@messagePermissionService.canInteract(#messageId, #currentUser, 'pin')")
    public ResponseEntity<PinnedMessageDto> pinMessage(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Pinning message {} by user {}", messageId, currentUser.getId());
        PinnedMessageDto pinnedMessage = pinnedMessageService.pinMessage(messageId, currentUser);
        return ResponseEntity.ok(pinnedMessage);
    }

    @DeleteMapping("/messages/{messageId}/pin")
    @PreAuthorize("@messagePermissionService.canInteract(#messageId, #currentUser, 'pin')")
    public ResponseEntity<ApiResponse> unpinMessage(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Unpinning message {} by user {}", messageId, currentUser.getId());
        pinnedMessageService.unpinMessage(messageId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Message unpinned successfully"));
    }

    @GetMapping("/conversations/{conversationId}/pinned-messages")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<List<PinnedMessageDto>> getPinnedMessages(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<PinnedMessageDto> pinnedMessages = pinnedMessageService.getPinnedMessages(conversationId, currentUser);
        return ResponseEntity.ok(pinnedMessages);
    }

    @GetMapping("/messages/{messageId}/pinned")
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<Boolean> isMessagePinned(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        boolean isPinned = pinnedMessageService.isMessagePinned(messageId, currentUser);
        return ResponseEntity.ok(isPinned);
    }
}