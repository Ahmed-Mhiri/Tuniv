package com.tuniv.backend.qa.dto;

import java.time.LocalDateTime;
import java.util.List;

public record QuestionResponseDto(
    Integer questionId,
    String title,
    String body,
    LocalDateTime createdAt,
    AuthorDto author,
    List<AnswerResponseDto> answers,
    int score,
    int currentUserVote
) {}