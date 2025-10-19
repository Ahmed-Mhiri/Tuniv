// MessageReactionsSummaryDto.java
package com.tuniv.backend.chat.dto.response;

import java.util.List;
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
    private List<String> topReactions; // Add this field
}