package com.tuniv.backend.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EditMessageRequest {
    
    @NotBlank(message = "Message body cannot be empty")
    @Size(max = 5000, message = "Message body cannot exceed 5000 characters")
    private String body;
}