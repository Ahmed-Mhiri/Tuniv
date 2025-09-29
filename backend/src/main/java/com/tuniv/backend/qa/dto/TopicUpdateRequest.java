package com.tuniv.backend.qa.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record TopicUpdateRequest(
    @NotBlank String title,
    String body,
    List<String> tags,
    List<Integer> attachmentIdsToDelete
) {}