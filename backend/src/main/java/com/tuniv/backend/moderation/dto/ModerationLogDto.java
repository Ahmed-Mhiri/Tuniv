package com.tuniv.backend.moderation.dto;

import com.tuniv.backend.user.dto.UserSummaryDto;
import java.time.Instant;

public record ModerationLogDto(
    Integer logId,
    UserSummaryDto moderator,
    String action, // e.g., "Banned User"
    UserSummaryDto targetUser,
    String targetContentSummary, // e.g., "Post #12345"
    String reason,
    Instant timestamp
) {}