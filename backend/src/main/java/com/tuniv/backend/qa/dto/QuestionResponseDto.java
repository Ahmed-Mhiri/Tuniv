package com.tuniv.backend.qa.dto;

import java.time.Instant;
import java.util.List;

import com.tuniv.backend.university.dto.ModuleDto;

public record QuestionResponseDto(
    Integer questionId,
    String title,
    String body,
    Instant createdAt,
    AuthorDto author,
    ModuleDto module, // ✅ ADD THIS FIELD
    List<AnswerResponseDto> answers,
    int score,
    int currentUserVote,
    List<AttachmentDto> attachments
) {}