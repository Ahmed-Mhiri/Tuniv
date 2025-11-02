package com.tuniv.backend.chat.controller;

import com.tuniv.backend.chat.dto.common.ReactionDto;
import com.tuniv.backend.chat.dto.request.ReactionRequestDto;
import com.tuniv.backend.chat.dto.response.MessageReactionsSummaryDto;
import com.tuniv.backend.chat.service.ReactionService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages/{messageId}/reactions")
@RequiredArgsConstructor
@Slf4j
public class MessageReactionController {

    private final ReactionService reactionService;

    @PostMapping
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<ReactionDto> addOrUpdateReaction(
            @PathVariable Integer messageId,
            @RequestBody @Valid ReactionRequestDto request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Adding reaction to message {} by user {}", messageId, currentUser.getId());
        ReactionDto reaction = reactionService.addOrUpdateReaction(messageId, request, currentUser);
        return ResponseEntity.ok(reaction);
    }

    @DeleteMapping
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<ApiResponse> removeReaction(
            @PathVariable Integer messageId,
            @RequestParam String emoji,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Removing reaction from message {} by user {}", messageId, currentUser.getId());
        reactionService.removeReaction(messageId, emoji, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Reaction removed successfully"));
    }

    @DeleteMapping("/{reactionId}")
    @PreAuthorize("@messagePermissionService.isReactionOwner(#reactionId, #currentUser)")
    public ResponseEntity<ApiResponse> removeReactionById(
            @PathVariable Integer messageId,
            @PathVariable Integer reactionId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Removing reaction {} by user {}", reactionId, currentUser.getId());
        reactionService.removeReactionById(reactionId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Reaction removed successfully"));
    }

    @GetMapping
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<List<ReactionDto>> getMessageReactions(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<ReactionDto> reactions = reactionService.getMessageReactions(messageId, currentUser);
        return ResponseEntity.ok(reactions);
    }

    @GetMapping("/summary")
    @PreAuthorize("@messagePermissionService.canView(#messageId, #currentUser)")
    public ResponseEntity<MessageReactionsSummaryDto> getMessageReactionsSummary(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        MessageReactionsSummaryDto summary = reactionService.getMessageReactionsSummary(messageId, currentUser);
        return ResponseEntity.ok(summary);
    }
}