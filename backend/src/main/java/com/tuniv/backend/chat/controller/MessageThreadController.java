package com.tuniv.backend.chat.controller;

import com.tuniv.backend.chat.dto.response.ChatMessageDto;
import com.tuniv.backend.chat.dto.response.MessageThreadDto;
import com.tuniv.backend.chat.service.MessageThreadService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
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
@RequestMapping("/api/v1/messages/{messageId}/thread")
@RequiredArgsConstructor
@Slf4j
public class MessageThreadController {

    private final MessageThreadService messageThreadService;

    @GetMapping("/replies")
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<Page<ChatMessageDto>> getMessageReplies(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ChatMessageDto> replies = messageThreadService.getMessageReplies(messageId, currentUser, pageable);
        return ResponseEntity.ok(replies);
    }

    @GetMapping
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<MessageThreadDto> getMessageThread(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        MessageThreadDto thread = messageThreadService.getMessageThread(messageId, currentUser);
        return ResponseEntity.ok(thread);
    }
}