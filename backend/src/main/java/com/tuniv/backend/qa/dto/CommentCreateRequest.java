package com.tuniv.backend.qa.dto;
import jakarta.validation.constraints.NotNull;


public record CommentCreateRequest(
    String body,
    Integer parentCommentId,

    // ✅ FIX: Add the answerId to link the comment to its parent answer.
    @NotNull Integer answerId
) {}