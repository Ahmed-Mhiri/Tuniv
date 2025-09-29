package com.tuniv.backend.qa.dto;

import java.util.List;

import com.tuniv.backend.qa.model.TopicType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TopicCreateRequest(
    @NotBlank String title,
    String body,
    @NotNull TopicType topicType,
    Integer moduleId,
    Integer communityId,
    List<String> tags
) {}
