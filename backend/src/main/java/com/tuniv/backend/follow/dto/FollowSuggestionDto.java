package com.tuniv.backend.follow.dto;

public record FollowSuggestionDto(
    // Reuses the core FollowableDto for the entity's info
    FollowableDto followable,
    
    // Explains why this suggestion is being made
    String reasonForSuggestion 
) {}