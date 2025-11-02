package com.tuniv.backend.chat.service;

import java.util.List;
import java.util.Map;

import com.tuniv.backend.chat.dto.common.ReactionDto;
import com.tuniv.backend.chat.dto.request.ReactionRequestDto;
import com.tuniv.backend.chat.dto.response.MessageReactionsSummaryDto;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.model.Reaction;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.user.model.User;

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
    
    /**
     * Fetches reaction counts for a list of message IDs in bulk.
     *
     * @param messageIds A list of message IDs.
     * @return A Map where the key is the messageId and the value is another
     * Map<String, Long> (Emoji -> Count).
     */
    Map<Integer, Map<String, Long>> getReactionCountsForMessages(List<Integer> messageIds);

    /**
     * Fetches the specific reactions for a single user across multiple messages.
     *
     * @param userId The ID of the user.
     * @param messageIds A list of message IDs.
     * @return A Map where the key is the messageId and the value is the
     * single emoji (as a String) that the user reacted with.
     */
    Map<Integer, String> getUserReactionsForMessages(Integer userId, List<Integer> messageIds);

    // ========== Additional Optimized Methods ==========

    /**
     * Gets all reaction details for a specific message (without user authentication).
     * Optimized version using projections.
     */
    List<ReactionDto> getMessageReactions(Integer messageId);

    /**
     * Adds a reaction to a message (internal use, without real-time events).
     */
    void addReaction(Message message, User user, String emoji);

    /**
     * Removes a specific reaction (internal use, without real-time events).
     */
    void removeReaction(Integer messageId, Integer userId, String emoji);

    /**
     * Gets reaction summary for a message (emoji counts).
     */
    Map<String, Long> getReactionSummary(Integer messageId);

    /**
     * Gets top emojis used in reactions for a message.
     */
    List<String> getTopEmojis(Integer messageId);
}