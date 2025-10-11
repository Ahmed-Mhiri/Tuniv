package com.tuniv.backend.community.dto;

import com.tuniv.backend.community.model.CommunityJoinPolicy;
import com.tuniv.backend.community.model.CommunityPostingPolicy;
import com.tuniv.backend.community.model.CommunityVisibility;

public record CommunitySettingsDto(
    CommunityJoinPolicy joinPolicy,
    CommunityPostingPolicy postingPolicy,
    CommunityVisibility visibility,
    boolean allowExternalMembers
) {}