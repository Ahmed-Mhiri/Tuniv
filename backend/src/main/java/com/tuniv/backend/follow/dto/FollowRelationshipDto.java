package com.tuniv.backend.follow.dto;

import com.tuniv.backend.user.dto.UserSummaryDto;

public record FollowRelationshipDto(
    // The other user in the relationship
    UserSummaryDto user,

    // Flags indicating the follow status
    boolean youFollowThem,
    boolean theyFollowYou
) {}