package com.tuniv.backend.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartConversationRequestDto {
    @NotNull
    private Integer participantId;
    // ✅ ADD THIS SETTER FOR DEBUGGING
    public void setParticipantId(Integer participantId) {
        System.out.println("--- DEBUG: Setting participantId to: " + participantId + " ---");
        this.participantId = participantId;
    }
}
