package com.tuniv.backend.qa.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.ReplyCreateRequest;
import com.tuniv.backend.qa.dto.ReplyResponseDto;
import com.tuniv.backend.qa.dto.ReplyUpdateRequest;
import com.tuniv.backend.qa.mapper.TopicMapper;
import com.tuniv.backend.qa.model.Reply;
import com.tuniv.backend.qa.service.ReplyService;
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReplyController {

    private final ReplyService replyService;

    /**
     * POST /api/v1/topics/{topicId}/replies
     * Creates a new reply (an answer or a comment) for a specific topic.
     */
    @PostMapping(value = "/topics/{topicId}/replies", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ReplyResponseDto> createReply(
            @PathVariable Integer topicId,
            @RequestPart("reply") @Valid ReplyCreateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        Reply savedReply = replyService.createReply(topicId, request, currentUser, files);

        // A new reply has no votes yet, so we pass an empty map for the vote status.
        ReplyResponseDto responseDto = TopicMapper.toReplyResponseDto(savedReply, currentUser, Collections.emptyMap());

        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * GET /api/v1/topics/{topicId}/replies
     * Retrieves replies for a topic. Can be filtered by type.
     * @param type (optional) "answer" or "comment" to filter the results. If omitted, all replies are returned.
     */
    @GetMapping("/topics/{topicId}/replies")
    public ResponseEntity<List<ReplyResponseDto>> getRepliesForTopic(
            @PathVariable Integer topicId,
            @RequestParam(required = false) String type,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        List<ReplyResponseDto> replies;
        if ("answer".equalsIgnoreCase(type)) {
            replies = replyService.getAnswersByTopic(topicId, currentUser);
        } else if ("comment".equalsIgnoreCase(type)) {
            replies = replyService.getCommentsByTopic(topicId, currentUser);
        } else {
            replies = replyService.getRepliesByTopic(topicId, currentUser);
        }

        return ResponseEntity.ok(replies);
    }

    /**
     * PUT /api/v1/replies/{replyId}
     * Updates the body and attachments of an existing reply. Requires ownership.
     */
    @PutMapping(value = "/replies/{replyId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ReplyResponseDto> updateReply(
            @PathVariable Integer replyId,
            @RequestPart("reply") @Valid ReplyUpdateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> newFiles,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        Reply updatedReply = replyService.updateReply(replyId, request, newFiles, currentUser);

        // Note: For simplicity, we map the single updated reply. A full refresh on the client
        // might be needed to get the latest vote counts for the entire thread.
        ReplyResponseDto responseDto = TopicMapper.toReplyResponseDto(updatedReply, currentUser, Collections.emptyMap());

        return ResponseEntity.ok(responseDto);
    }

    /**
     * DELETE /api/v1/replies/{replyId}
     * Deletes a reply. Requires ownership and that the reply is not an accepted solution.
     */
    @DeleteMapping("/replies/{replyId}")
    public ResponseEntity<Void> deleteReply(
            @PathVariable Integer replyId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        replyService.deleteReply(replyId, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/replies/{parentReplyId}/comments
     * Retrieves the list of nested comments for a specific parent reply.
     */
    @GetMapping("/replies/{parentReplyId}/comments")
    public ResponseEntity<List<ReplyResponseDto>> getNestedComments(
            @PathVariable Integer parentReplyId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        List<ReplyResponseDto> nestedComments = replyService.getNestedComments(parentReplyId, currentUser);
        return ResponseEntity.ok(nestedComments);
    }

    /**
     * GET /api/v1/replies/{replyId}/can-be-solution
     * Checks if the current user has the authority to mark this reply as a solution.
     */
    @GetMapping("/replies/{replyId}/can-be-solution")
    public ResponseEntity<Map<String, Boolean>> checkCanMarkAsSolution(
            @PathVariable Integer replyId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        boolean canMark = replyService.canMarkAsSolution(replyId, currentUser);
        return ResponseEntity.ok(Map.of("canMarkAsSolution", canMark));
    }
}
