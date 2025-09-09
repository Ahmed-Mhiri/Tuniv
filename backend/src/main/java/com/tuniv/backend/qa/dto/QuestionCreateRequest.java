package com.tuniv.backend.qa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuestionCreateRequest(
    @NotBlank @Size(min = 10) String title,
    @NotBlank @Size(min = 20) String body,
    
    // ✅ FIX: Add the moduleId so the controller knows which module the question belongs to.
    @NotNull Integer moduleId 
) {}