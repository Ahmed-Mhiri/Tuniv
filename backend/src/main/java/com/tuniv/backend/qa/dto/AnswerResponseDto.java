package com.tuniv.backend.qa.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AnswerResponseDto(
    Integer answerId,
    String body,
    Boolean isSolution,
    LocalDateTime createdAt,
    AuthorDto author,
    int score,
    int currentUserVote,
    List<CommentResponseDto> comments // The list of top-level comments
) {}