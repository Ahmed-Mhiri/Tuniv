package com.tuniv.backend.follow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tuniv.backend.follow.model.FeedItemType;
import com.tuniv.backend.user.dto.UserSummaryDto;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FollowFeedItemDto(
    // The entity that generated this feed item (e.g., the Community a topic was posted in)
    FollowableDto source,
    
    // The user who performed the action (e.g., the author of the topic)
    UserSummaryDto actor,
    
    FeedItemType itemType,
    Instant timestamp,
    
    // The actual content, e.g., a TopicSummaryDto. 
    // Using Object allows for flexibility.
    Object content 
) {}