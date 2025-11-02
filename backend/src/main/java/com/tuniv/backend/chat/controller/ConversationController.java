package com.tuniv.backend.chat.controller;

import com.tuniv.backend.chat.dto.*;
import com.tuniv.backend.chat.dto.request.CreateGroupRequest;
import com.tuniv.backend.chat.dto.request.StartConversationRequestDto;
import com.tuniv.backend.chat.dto.request.UpdateGroupInfoRequest;
import com.tuniv.backend.chat.dto.response.ConversationDetailDto;
import com.tuniv.backend.chat.dto.response.ConversationSummaryDto;
import com.tuniv.backend.chat.service.ConversationService;
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

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

    private final ConversationService conversationService;

    // ========== Conversation Lifecycle ==========

    @PostMapping("/direct")
    public ResponseEntity<ConversationDetailDto> startDirectConversation(
            @RequestBody @Valid StartConversationRequestDto request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Starting direct conversation between user {} and {}", currentUser.getId(), request.getTargetUserId());
        ConversationDetailDto conversation = conversationService.startDirectConversation(request, currentUser);
        return ResponseEntity.ok(conversation);
    }

    @PostMapping("/group")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConversationDetailDto> createGroupConversation(
            @RequestBody @Valid CreateGroupRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Creating group conversation '{}' by user {}", request.getTitle(), currentUser.getId());
        ConversationDetailDto conversation = conversationService.createGroupConversation(request, currentUser);
        return ResponseEntity.ok(conversation);
    }

    @PostMapping("/{conversationId}/archive")
    @PreAuthorize("@conversationPermissionService.hasPermission(#conversationId, #currentUser, 'archive_conversation')")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<ApiResponse> archiveConversation(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Archiving conversation {} by user {}", conversationId, currentUser.getId());
        conversationService.archiveConversation(conversationId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Conversation archived successfully"));
    }

    @DeleteMapping("/{conversationId}")
    @PreAuthorize("@conversationPermissionService.hasPermission(#conversationId, #currentUser, 'delete_conversation')")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<ApiResponse> deleteConversation(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Deleting conversation {} by user {}", conversationId, currentUser.getId());
        conversationService.deleteConversation(conversationId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Conversation deleted successfully"));
    }

    @PostMapping("/{conversationId}/restore")
    @PreAuthorize("@conversationPermissionService.hasPermission(#conversationId, #currentUser, 'archive_conversation')")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<ApiResponse> restoreConversation(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Restoring conversation {} by user {}", conversationId, currentUser.getId());
        conversationService.restoreConversation(conversationId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Conversation restored successfully"));
    }

    // ========== Data Retrieval ==========

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ConversationSummaryDto>> getMyConversations(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching conversations for user {}", currentUser.getId());
        Page<ConversationSummaryDto> conversations = conversationService.getMyConversations(currentUser, pageable);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/{conversationId}")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<ConversationDetailDto> getConversationDetails(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.debug("Fetching details for conversation {} by user {}", conversationId, currentUser.getId());
        ConversationDetailDto conversation = conversationService.getConversationDetails(conversationId, currentUser);
        return ResponseEntity.ok(conversation);
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ConversationSummaryDto>> searchConversations(
            @RequestParam String query,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Searching conversations with query '{}' for user {}", query, currentUser.getId());
        Page<ConversationSummaryDto> conversations = conversationService.searchConversations(query, currentUser, pageable);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/archived")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ConversationSummaryDto>> getArchivedConversations(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching archived conversations for user {}", currentUser.getId());
        Page<ConversationSummaryDto> conversations = conversationService.getArchivedConversations(currentUser, pageable);
        return ResponseEntity.ok(conversations);
    }

    // ========== Conversation Settings ==========

    @PutMapping("/{conversationId}/info")
    @PreAuthorize("@conversationPermissionService.hasPermission(#conversationId, #currentUser, 'edit_conversation_info')")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<ConversationDetailDto> updateGroupInfo(
            @PathVariable Integer conversationId,
            @RequestBody @Valid UpdateGroupInfoRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Updating group info for conversation {} by user {}", conversationId, currentUser.getId());
        ConversationDetailDto conversation = conversationService.updateGroupInfo(conversationId, request, currentUser);
        return ResponseEntity.ok(conversation);
    }

    // ========== Utility Endpoints ==========

    @GetMapping("/direct/exists")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> directConversationExists(
            @RequestParam Integer user1Id,
            @RequestParam Integer user2Id) {
        boolean exists = conversationService.directConversationExists(user1Id, user2Id);
        return ResponseEntity.ok(exists);
    }
}