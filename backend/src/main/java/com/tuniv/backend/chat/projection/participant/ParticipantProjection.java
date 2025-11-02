package com.tuniv.backend.chat.projection.participant;

import java.time.Instant;

public interface ParticipantProjection {
    Integer getUserId();
    String getUsername();
    String getProfilePhotoUrl();
    String getNickname();
    Integer getRoleId();
    String getRoleName();
    String getDisplayName();
    Instant getJoinedAt();
    Instant getLastActiveAt();
    Integer getMessageCount();
    Boolean getIsActive();
    Boolean getNotificationsEnabled();
    Boolean getIsMuted();
    Instant getMutedUntil();
    Integer getUnreadCount();
    Instant getLastReadTimestamp();
}