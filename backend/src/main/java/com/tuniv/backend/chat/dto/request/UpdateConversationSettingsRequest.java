package com.tuniv.backend.chat.dto.request;

import java.time.Instant;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConversationSettingsRequest {
    
    @Size(max = 50, message = "Nickname cannot exceed 50 characters")
    private String nickname;
    
    private Boolean notificationsEnabled;
    
    private Boolean isMuted;
    
    private Instant mutedUntil;
}