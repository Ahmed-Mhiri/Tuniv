package com.tuniv.backend.follow.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkFollowRequest(
    // A list of the followable_id for each entity to follow/unfollow.
    @NotEmpty
    List<Integer> followableIds
) {}