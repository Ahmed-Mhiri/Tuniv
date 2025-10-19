package com.tuniv.backend.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StartConversationRequestDto {
    
    @NotNull(message = "Target user ID is required")
    private Integer targetUserId;
}