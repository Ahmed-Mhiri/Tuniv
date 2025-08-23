package com.tuniv.backend.qa.dto;

import java.time.LocalDateTime;

public record AnswerResponseDto(
    Integer answerId,
    String body,
    Boolean isSolution,
    LocalDateTime createdAt,
    AuthorDto author
) {}