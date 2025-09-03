package com.tuniv.backend.qa.dto;

import java.time.Instant;
import java.util.List;

public record AnswerResponseDto(
    Integer answerId,
    String body,
    Boolean isSolution,
    Instant createdAt,
    AuthorDto author,
    int score,
    int currentUserVote,
    List<CommentResponseDto> comments,
    List<AttachmentDto> attachments // <-- ADD THIS

) {}