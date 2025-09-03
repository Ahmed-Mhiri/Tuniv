package com.tuniv.backend.qa.dto;

import java.time.Instant;
import java.util.List;

public record QuestionResponseDto(
    Integer questionId,
    String title,
    String body,
    Instant createdAt,
    AuthorDto author,
    List<AnswerResponseDto> answers,
    int score,
    int currentUserVote,
    List<AttachmentDto> attachments // <-- ADD THIS

) {}