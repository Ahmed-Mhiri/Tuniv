// MessageReactionsSummaryDto.java
package com.tuniv.backend.chat.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageReactionsSummaryDto {
    private Integer messageId;
    private Map<String, Long> reactionCounts; // emoji -> count
    private Map<String, Boolean> userReactions; // emoji -> hasReacted
    private Integer totalReactions;
}