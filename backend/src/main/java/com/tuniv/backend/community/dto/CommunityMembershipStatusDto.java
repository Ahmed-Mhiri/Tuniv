package com.tuniv.backend.community.dto;

import com.tuniv.backend.community.model.CommunityRole;
import java.time.Instant;

public record CommunityMembershipStatusDto(
    boolean isMember,
    CommunityRole role,
    boolean isBanned,
    Instant muteUntil,
    boolean notificationsEnabled
) {}
