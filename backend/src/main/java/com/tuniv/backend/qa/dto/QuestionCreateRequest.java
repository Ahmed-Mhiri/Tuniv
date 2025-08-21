package com.tuniv.backend.qa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuestionCreateRequest(
    @NotBlank @Size(min = 10) String title,
    @NotBlank @Size(min = 20) String body
) {}