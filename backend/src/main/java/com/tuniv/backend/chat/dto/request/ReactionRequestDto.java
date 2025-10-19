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
public class ReactionRequestDto {
    
    @NotBlank(message = "Emoji is required")
    @Size(max = 10, message = "Emoji cannot exceed 10 characters")
    private String emoji;
    
    private Integer skinTone;
    
    @Size(max = 100, message = "Custom text cannot exceed 100 characters")
    private String customText;
}