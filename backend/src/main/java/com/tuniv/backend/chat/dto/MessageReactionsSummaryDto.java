package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class MessageReactionsSummaryDto {
    private Integer messageId;
    private Map<String, Long> reactionCounts; // emoji -> count
    private Map<String, Boolean> userReactions; // emoji -> hasReacted
    private Integer totalReactions;
}