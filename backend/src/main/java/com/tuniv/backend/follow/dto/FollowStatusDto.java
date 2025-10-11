package com.tuniv.backend.follow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FollowStatusDto(
    boolean isFollowing,
    int followersCount,
    
    // Only applicable if the entity being checked is a User
    Integer followingCount 
) {}