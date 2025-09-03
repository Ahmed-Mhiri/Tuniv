package com.tuniv.backend.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartConversationRequestDto {
    @NotNull
    private Integer participantId;
}
