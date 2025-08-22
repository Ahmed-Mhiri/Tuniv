package com.tuniv.backend.qa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
    @NotBlank
    @Size(min = 1, max = 2000)
    String body
) {}