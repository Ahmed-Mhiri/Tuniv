package com.tuniv.backend.qa.dto;

import java.time.Instant;
import java.util.List;

public record CommentResponseDto(
    Integer commentId,
    String body,
    Instant createdAt,
    AuthorDto author, // Use AuthorDto for consistency
    int score,
    int currentUserVote, // 1 for upvote, -1 for downvote, 0 for no vote
    List<CommentResponseDto> children, // For nested replies
    List<AttachmentDto> attachments // <-- ADD THIS

) {}