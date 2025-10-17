package com.tuniv.backend.chat.service;

import com.tuniv.backend.chat.dto.MessageReactionsSummaryDto;
import com.tuniv.backend.chat.model.Reaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReactionService {
    
    private final ReactionMapper reactionMapper;

    public MessageReactionsSummaryDto calculateReactionsSummary(
        List<Reaction> reactions, 
        Integer currentUserId,
        Integer messageId
    ) {
        if (reactions == null || reactions.isEmpty()) {
            return reactionMapper.toMessageReactionsSummaryDto(
                Map.of(), Map.of(), 0, messageId
            );
        }
        
        // Group by emoji and count
        Map<String, Long> reactionCounts = reactions.stream()
            .collect(Collectors.groupingBy(
                Reaction::getEmoji,
                Collectors.counting()
            ));
        
        // Check if current user has reacted with each emoji
        Map<String, Boolean> userReactions = Map.of();
        if (currentUserId != null) {
            userReactions = reactions.stream()
                .filter(reaction -> reaction.getUser().getUserId().equals(currentUserId))
                .collect(Collectors.toMap(
                    Reaction::getEmoji,
                    reaction -> true,
                    (existing, replacement) -> existing
                ));
        }
        
        return reactionMapper.toMessageReactionsSummaryDto(
            reactionCounts,
            userReactions,
            reactions.size(),
            messageId
        );
    }
}