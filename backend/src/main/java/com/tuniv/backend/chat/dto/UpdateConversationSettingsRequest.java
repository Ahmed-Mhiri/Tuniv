package com.tuniv.backend.chat.dto;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateConversationSettingsRequest {
    private String nickname;
    private Boolean notificationsEnabled;
    private Boolean isMuted;
    private Instant mutedUntil; // Optional: specific mute duration
}