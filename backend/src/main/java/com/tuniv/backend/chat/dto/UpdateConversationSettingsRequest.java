// UpdateConversationSettingsRequest.java
package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConversationSettingsRequest {
    private String nickname;
    private Boolean notificationsEnabled;
    private Boolean isMuted;
    private Instant mutedUntil;
}