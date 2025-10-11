package com.tuniv.backend.moderation.dto;

public record ModerationActionDto(
    String actionType, // e.g., "WARN_USER", "DELETE_POST", "BAN_USER"
    String reason,
    boolean notifyUser,
    Long banDurationInHours // Optional: for temporary bans
) {}