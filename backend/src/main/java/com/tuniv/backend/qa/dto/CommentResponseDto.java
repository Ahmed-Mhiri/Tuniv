package com.tuniv.backend.qa.dto;

import java.time.LocalDateTime;

public record CommentResponseDto(
    Integer commentId,
    String body,
    LocalDateTime createdAt,
    String authorUsername
) {}