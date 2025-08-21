package com.tuniv.backend.qa.dto;

import jakarta.validation.constraints.NotBlank;

public record AnswerCreateRequest(
    @NotBlank String body
) {}