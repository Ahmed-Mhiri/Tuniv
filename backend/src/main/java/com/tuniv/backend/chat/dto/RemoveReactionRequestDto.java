package com.tuniv.backend.chat.dto;

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
public class RemoveReactionRequestDto {
    
    @NotBlank(message = "Emoji is required")
    @Size(max = 10, message = "Emoji cannot exceed 10 characters")
    private String emoji;
}