package com.tuniv.backend.community.dto;

import java.time.Instant;

public record ModerationStatusDto(
    boolean isBanned,
    Instant muteUntil,
    String banReason,
    Instant bannedAt
) {}