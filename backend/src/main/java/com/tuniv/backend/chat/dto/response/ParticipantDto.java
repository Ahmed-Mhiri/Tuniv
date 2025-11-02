// ParticipantDto.java
package com.tuniv.backend.chat.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDto {
    private Integer userId;
    private String username;
    private String profilePhotoUrl;
    private String nickname;
    private Integer roleId;
    private String roleName;
    private String displayName;
    private Instant joinedAt;
    private Instant lastActiveAt;
    private Integer messageCount;
    private boolean isActive;
    private boolean notificationsEnabled;
    private boolean isMuted;
    private Instant mutedUntil;
    private Integer unreadCount;
    private Instant lastReadTimestamp;
}