package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
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
