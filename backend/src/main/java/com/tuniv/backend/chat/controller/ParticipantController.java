package com.tuniv.backend.chat.controller;

import com.tuniv.backend.chat.dto.*;
import com.tuniv.backend.chat.service.ParticipantService;
import com.tuniv.backend.chat.model.MuteDuration;
import com.tuniv.backend.chat.annotation.RequiresMembership;
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
@RequestMapping("/api/v1/conversations/{conversationId}/participants")
@RequiredArgsConstructor
@Slf4j
public class ParticipantController {

    private final ParticipantService participantService;

    // ========== Participant Management ==========

    @PostMapping
    @PreAuthorize("@conversationPermissionService.hasPermission(#conversationId, #currentUser, 'add_participants')")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<List<ParticipantDto>> addParticipants(
            @PathVariable Integer conversationId,
            @RequestBody @Valid UpdateParticipantsRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Adding participants to conversation {} by user {}", conversationId, currentUser.getId());
        List<ParticipantDto> participants = participantService.addParticipants(conversationId, request, currentUser);
        return ResponseEntity.ok(participants);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("@conversationPermissionService.canRemoveParticipant(#conversationId, #userId, #currentUser)")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<ApiResponse> removeParticipant(
            @PathVariable Integer conversationId,
            @PathVariable Integer userId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Removing participant {} from conversation {} by user {}", userId, conversationId, currentUser.getId());
        participantService.removeParticipant(conversationId, userId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Participant removed successfully"));
    }

    @PostMapping("/leave")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<ApiResponse> leaveConversation(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("User {} leaving conversation {}", currentUser.getId(), conversationId);
        participantService.leaveConversation(conversationId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Left conversation successfully"));
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("@conversationPermissionService.hasPermission(#conversationId, #currentUser, 'manage_roles')")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<ParticipantDto> updateParticipantRole(
            @PathVariable Integer conversationId,
            @PathVariable Integer userId,
            @RequestParam Integer roleId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Updating role for user {} in conversation {} by user {}", userId, conversationId, currentUser.getId());
        ParticipantDto participant = participantService.updateParticipantRole(conversationId, userId, roleId, currentUser);
        return ResponseEntity.ok(participant);
    }

    @GetMapping
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<List<ParticipantDto>> getConversationParticipants(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.debug("Fetching participants for conversation {} by user {}", conversationId, currentUser.getId());
        List<ParticipantDto> participants = participantService.getConversationParticipants(conversationId, currentUser);
        return ResponseEntity.ok(participants);
    }

    @GetMapping("/me")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<ParticipantDto> getMyParticipantInfo(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.debug("Fetching participant info for user {} in conversation {}", currentUser.getId(), conversationId);
        ParticipantDto participant = participantService.getMyParticipantInfo(conversationId, currentUser);
        return ResponseEntity.ok(participant);
    }

    // ========== Participant Settings ==========

    @PutMapping("/me/settings")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<ParticipantDto> updateMyConversationSettings(
            @PathVariable Integer conversationId,
            @RequestBody @Valid UpdateConversationSettingsRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Updating conversation settings for user {} in conversation {}", currentUser.getId(), conversationId);
        ParticipantDto participant = participantService.updateMyConversationSettings(conversationId, request, currentUser);
        return ResponseEntity.ok(participant);
    }

    // ========== Moderation Actions ==========

    @PostMapping("/{userId}/mute")
    @PreAuthorize("@conversationPermissionService.hasPermission(#conversationId, #currentUser, 'mute_participants')")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<ParticipantDto> muteParticipant(
            @PathVariable Integer conversationId,
            @PathVariable Integer userId,
            @RequestParam MuteDuration duration,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Muting user {} in conversation {} by user {}", userId, conversationId, currentUser.getId());
        ParticipantDto participant = participantService.muteParticipant(conversationId, userId, duration, currentUser);
        return ResponseEntity.ok(participant);
    }

    @PostMapping("/{userId}/unmute")
    @PreAuthorize("@conversationPermissionService.hasPermission(#conversationId, #currentUser, 'mute_participants')")
    @RequiresMembership(conversationIdParam = "conversationId")
    public ResponseEntity<ParticipantDto> unmuteParticipant(
            @PathVariable Integer conversationId,
            @PathVariable Integer userId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        log.info("Unmuting user {} in conversation {} by user {}", userId, conversationId, currentUser.getId());
        ParticipantDto participant = participantService.unmuteParticipant(conversationId, userId, currentUser);
        return ResponseEntity.ok(participant);
    }

}