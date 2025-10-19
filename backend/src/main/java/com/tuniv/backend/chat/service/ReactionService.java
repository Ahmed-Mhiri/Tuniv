package com.tuniv.backend.chat.service;

import java.util.List;

import com.tuniv.backend.chat.dto.MessageReactionsSummaryDto;
import com.tuniv.backend.chat.dto.ReactionDto;
import com.tuniv.backend.chat.dto.ReactionRequestDto;
import com.tuniv.backend.chat.model.Reaction;
import com.tuniv.backend.config.security.services.UserDetailsImpl;

public interface ReactionService {

    /**
     * Adds or updates a user's reaction to a message.
     */
    ReactionDto addOrUpdateReaction(Integer messageId, ReactionRequestDto request, UserDetailsImpl currentUser);
    
    /**
     * Removes a reaction from a message based on the emoji.
     */
    void removeReaction(Integer messageId, String emoji, UserDetailsImpl currentUser);
    
    /**
     * Removes a specific reaction by its unique ID.
     */
    void removeReactionById(Integer reactionId, UserDetailsImpl currentUser);
    
    /**
     * Gets all reaction details (who reacted with what) for a specific message.
     */
    List<ReactionDto> getMessageReactions(Integer messageId, UserDetailsImpl currentUser);
    
    /**
     * Gets a summary of reactions for a message (counts by emoji, and if current user reacted).
     */
    MessageReactionsSummaryDto getMessageReactionsSummary(Integer messageId, UserDetailsImpl currentUser);
    
    /**
     * Calculates reaction summary from a list of reactions.
     * This method can be used internally and by other services that need reaction summaries.
     */
    MessageReactionsSummaryDto calculateReactionsSummary(List<Reaction> reactions, Integer currentUserId, Integer messageId);
}