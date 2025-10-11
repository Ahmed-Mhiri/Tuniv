package com.tuniv.backend.follow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tuniv.backend.follow.model.FollowableType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FollowableDto(
    // The actual ID of the entity (e.g., userId, communityId)
    Integer entityId,
    
    FollowableType type,
    String name,
    String imageUrl,
    
    // A brief, contextual detail (e.g., user's reputation or community's member count)
    String detail, 
    
    // Indicates if the currently authenticated user is following this entity
    boolean isFollowedByCurrentUser 
) {}