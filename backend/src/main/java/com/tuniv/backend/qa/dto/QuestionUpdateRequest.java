package com.tuniv.backend.qa.dto;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record QuestionUpdateRequest(
    @NotBlank @Size(min = 10) String title,
    @NotBlank @Size(min = 20) String body,
    List<Integer> attachmentIdsToDelete,
    List<String> tags // ✅ Add this

) {}
