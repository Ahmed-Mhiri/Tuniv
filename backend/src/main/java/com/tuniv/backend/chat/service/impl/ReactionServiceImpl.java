package com.tuniv.backend.chat.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.chat.dto.common.ReactionAction;
import com.tuniv.backend.chat.dto.common.ReactionDto;
import com.tuniv.backend.chat.dto.event.MessageReactionUpdateDto;
import com.tuniv.backend.chat.dto.request.ReactionRequestDto;
import com.tuniv.backend.chat.dto.response.MessageReactionsSummaryDto;
import com.tuniv.backend.chat.mapper.mapstruct.ReactionMapper;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.model.Reaction;
import com.tuniv.backend.chat.projection.reaction.ReactionProjection;
import com.tuniv.backend.chat.projection.reaction.ReactionSummaryProjection;
import com.tuniv.backend.chat.repository.ReactionRepository;
import com.tuniv.backend.chat.service.ChatRealtimeService;
import com.tuniv.backend.chat.service.EntityFinderService;
import com.tuniv.backend.chat.service.ReactionService;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReactionServiceImpl implements ReactionService {

    private final ReactionRepository reactionRepository;
    private final ReactionMapper reactionMapper;
    private final EntityFinderService entityFinderService;
    private final ChatRealtimeService chatRealtimeService;

    @Override
    public ReactionDto addOrUpdateReaction(Integer messageId, ReactionRequestDto request, UserDetailsImpl currentUser) {
        log.info("Adding/updating reaction '{}' to message {} by user {}", request.getEmoji(), messageId, currentUser.getId());
        
        // ✅ VERIFIED: Uses getNonDeletedMessageOrThrow for message
        Message message = entityFinderService.getNonDeletedMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        User currentUserEntity = entityFinderService.getUserOrThrow(currentUser.getId());
        
        // ✅ VERIFIED: Uses repository method with explicit isRemoved = false filter
        Optional<Reaction> existingReactionOpt = reactionRepository.findByMessageAndUserAndEmojiAndIsRemovedFalse(
            message, currentUserEntity, request.getEmoji());
        
        Reaction reaction;
        if (existingReactionOpt.isPresent()) {
            reaction = existingReactionOpt.get();
            log.debug("Updating existing reaction for message {} by user {}", messageId, currentUser.getId());
        } else {
            reaction = new Reaction();
            reaction.setMessage(message);
            reaction.setUser(currentUserEntity);
            reaction.setEmoji(request.getEmoji());
            reaction.setSkinTone(request.getSkinTone());
            reaction.setCustomText(request.getCustomText());
            if (message.getUniversityContext() != null) {
                reaction.setUniversityContext(message.getUniversityContext());
            }
            log.debug("Creating new reaction for message {} by user {}", messageId, currentUser.getId());
        }
        
        Reaction savedReaction = reactionRepository.save(reaction);
        ReactionDto reactionDto = reactionMapper.toReactionDto(savedReaction);
        
        // Broadcast reaction update to all conversation participants
        MessageReactionUpdateDto reactionUpdate = new MessageReactionUpdateDto();
        reactionUpdate.setMessageId(messageId);
        reactionUpdate.setReaction(reactionDto);
        reactionUpdate.setAction(ReactionAction.ADDED);
        
        chatRealtimeService.broadcastReactionUpdate(message.getConversation().getConversationId(), reactionUpdate);
        log.info("Reaction '{}' added/updated successfully for message {}", request.getEmoji(), messageId);
        
        return reactionDto;
    }

    @Override
    public void removeReaction(Integer messageId, String emoji, UserDetailsImpl currentUser) {
        log.info("Removing reaction '{}' from message {} by user {}", emoji, messageId, currentUser.getId());
        
        // ✅ VERIFIED: Uses getNonDeletedMessageOrThrow for message
        Message message = entityFinderService.getNonDeletedMessageOrThrow(messageId);
        User currentUserEntity = entityFinderService.getUserOrThrow(currentUser.getId());
        
        // ✅ VERIFIED: Uses repository method with explicit isRemoved = false filter
        Optional<Reaction> reactionOpt = reactionRepository.findByMessageAndUserAndEmojiAndIsRemovedFalse(
            message, currentUserEntity, emoji);
        
        if (reactionOpt.isPresent()) {
            Reaction reaction = reactionOpt.get();
            reaction.setRemoved(true);
            reaction.setRemovedAt(Instant.now());
            reactionRepository.save(reaction);

            ReactionDto reactionDto = reactionMapper.toReactionDto(reaction);
            
            MessageReactionUpdateDto reactionUpdate = new MessageReactionUpdateDto();
            reactionUpdate.setMessageId(messageId);
            reactionUpdate.setReaction(reactionDto);
            reactionUpdate.setAction(ReactionAction.REMOVED);
            
            chatRealtimeService.broadcastReactionUpdate(message.getConversation().getConversationId(), reactionUpdate);
            log.info("Reaction '{}' removed from message {} by user {}", emoji, messageId, currentUser.getId());
        } else {
            log.warn("Reaction '{}' not found for message {} by user {}", emoji, messageId, currentUser.getId());
            throw new ResourceNotFoundException("Reaction not found");
        }
    }

    @Override
    public void removeReactionById(Integer reactionId, UserDetailsImpl currentUser) {
        log.info("Removing reaction by ID {} by user {}", reactionId, currentUser.getId());

        // ✅ UPDATED: Use getReactionByIdEvenIfRemoved instead of getReactionOrThrow
        Reaction reaction = entityFinderService.getReactionByIdEvenIfRemoved(reactionId);
        
        // Validate ownership
        if (!reaction.getUser().getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only remove your own reactions.");
        }

        // Check if already removed
        if (reaction.isRemoved()) {
            log.warn("Reaction {} is already removed", reactionId);
            return;
        }

        reaction.setRemoved(true);
        reaction.setRemovedAt(Instant.now());
        reactionRepository.save(reaction);

        ReactionDto reactionDto = reactionMapper.toReactionDto(reaction);

        MessageReactionUpdateDto reactionUpdate = new MessageReactionUpdateDto();
        reactionUpdate.setMessageId(reaction.getMessage().getId());
        reactionUpdate.setReaction(reactionDto);
        reactionUpdate.setAction(ReactionAction.REMOVED);

        chatRealtimeService.broadcastReactionUpdate(
            reaction.getMessage().getConversation().getConversationId(), 
            reactionUpdate
        );
        
        log.info("Reaction {} removed successfully", reactionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReactionDto> getMessageReactions(Integer messageId, UserDetailsImpl currentUser) {
        log.debug("Fetching all reactions for message {} by user {}", messageId, currentUser.getId());
        
        // ✅ VERIFIED: Uses getNonDeletedMessageOrThrow for message
        Message message = entityFinderService.getNonDeletedMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        
        // ✅ VERIFIED: Uses repository method with explicit isRemoved = false filter
        List<Reaction> reactions = reactionRepository.findByMessageAndIsRemovedFalse(message);
        List<ReactionDto> reactionDtos = reactionMapper.toReactionDtoList(reactions);
        
        log.debug("Found {} reactions for message {}", reactionDtos.size(), messageId);
        return reactionDtos;
    }

    @Override
    @Transactional(readOnly = true)
    public MessageReactionsSummaryDto getMessageReactionsSummary(Integer messageId, UserDetailsImpl currentUser) {
        log.debug("Fetching reaction summary for message {} by user {}", messageId, currentUser.getId());
        
        // ✅ VERIFIED: Uses getNonDeletedMessageOrThrow for message
        Message message = entityFinderService.getNonDeletedMessageOrThrow(messageId);
        validateConversationMembership(message.getConversation(), currentUser);
        
        // ✅ VERIFIED: Uses repository method with explicit isRemoved = false filter
        List<Reaction> reactions = reactionRepository.findByMessageAndIsRemovedFalse(message);
        MessageReactionsSummaryDto summary = calculateReactionsSummary(reactions, currentUser.getId(), messageId);
        
        log.debug("Reaction summary for message {}: {} reaction types", messageId, 
                 summary.getReactionCounts() != null ? summary.getReactionCounts().size() : 0);
        return summary;
    }

    // ========== Optimized Methods ==========

    @Override
    @Transactional(readOnly = true)
    public List<ReactionDto> getMessageReactions(Integer messageId) {
        log.debug("Fetching reactions for message {}", messageId);
        
        // Use projection for better performance
        List<ReactionProjection> projections = reactionRepository
            .findActiveReactionsByMessageId(messageId);
        
        return projections.stream()
            .map(projection -> new ReactionDto(
                projection.getId(),
                projection.getMessageId(),
                projection.getUserId(),
                projection.getUsername(),
                projection.getProfilePhotoUrl(),
                projection.getEmoji(),
                projection.getSkinTone(),
                projection.getCustomText(),
                projection.getCreatedAt(),
                projection.getIsRemoved(),
                projection.getRemovedAt()
            ))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Map<String, Long>> getReactionCountsForMessages(List<Integer> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Map.of();
        }
        
        log.debug("Fetching reaction counts for {} messages", messageIds.size());
        
        // 1. Use your existing repository projection
        List<ReactionSummaryProjection> summaries = reactionRepository
            .getReactionCountsByMessageIds(messageIds);

        // 2. Convert the list of projections into the nested map
        return summaries.stream()
            .collect(Collectors.groupingBy(
                ReactionSummaryProjection::getMessageId,
                Collectors.toMap(
                    ReactionSummaryProjection::getEmoji,
                    ReactionSummaryProjection::getCount
                )
            ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, String> getUserReactionsForMessages(Integer userId, List<Integer> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Map.of();
        }

        log.debug("Fetching user reactions for user {} across {} messages", userId, messageIds.size());
        
        // 1. Use your existing repository projection
        List<ReactionProjection> userReactions = reactionRepository
            .findUserReactionsForMessages(userId, messageIds);

        // 2. Convert to a simple Map<MessageID, Emoji>
        // (Assuming a user can only have one reaction type per message, 
        //  which findUserReactionsForMessages implies. If they can have multiple,
        //  this should be Collectors.toMap(..., ..., (old, new) -> old) or Collectors.groupingBy)
        return userReactions.stream()
            .collect(Collectors.toMap(
                ReactionProjection::getMessageId,
                ReactionProjection::getEmoji,
                (existingEmoji, newEmoji) -> existingEmoji // Handle potential duplicates
            ));
    }

    @Override
    @Transactional
    public void addReaction(Message message, User user, String emoji) {
        // 1. Find ANY reaction (removed or not)
        Optional<Reaction> existingReactionOpt = reactionRepository.findByMessageAndUserAndEmoji(message, user, emoji);

        if (existingReactionOpt.isPresent()) {
            Reaction existingReaction = existingReactionOpt.get();
            // 2. If found...
            if (existingReaction.isRemoved()) {
                // ...and it was removed, reactivate it.
                existingReaction.setRemoved(false);
                existingReaction.setRemovedAt(null);
                reactionRepository.save(existingReaction);
                log.debug("Reactivated reaction {} for message {} by user {}", emoji, message.getId(), user.getUserId());
            } else {
                // ...and it's already active, do nothing.
                log.debug("Reaction {} already active for message {} by user {}", emoji, message.getId(), user.getUserId());
            }
        } else {
            // 3. If not found, create a new one.
            Reaction reaction = new Reaction();
            reaction.setMessage(message);
            reaction.setUser(user);
            reaction.setEmoji(emoji);
            reaction.setRemoved(false); // Explicitly set as active

            if (message.getUniversityContext() != null) {
                reaction.setUniversityContext(message.getUniversityContext());
            }

            reactionRepository.save(reaction);
            log.debug("Added new reaction {} to message {} by user {}", emoji, message.getId(), user.getUserId());
        }
    }

    @Override
    @Transactional
    public void removeReaction(Integer messageId, Integer userId, String emoji) {
        reactionRepository.removeSpecificReaction(messageId, userId, emoji);
        log.debug("Removed reaction {} from message {} by user {}", emoji, messageId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getReactionSummary(Integer messageId) {
        List<ReactionSummaryProjection> summaries = reactionRepository
            .getReactionSummaryByMessageId(messageId);
        
        return summaries.stream()
            .collect(Collectors.toMap(
                ReactionSummaryProjection::getEmoji,
                ReactionSummaryProjection::getCount
            ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getTopEmojis(Integer messageId) {
        List<Object[]> results = reactionRepository.findTopEmojisByMessageId(messageId);
        return results.stream()
            .map(result -> (String) result[0])
            .collect(Collectors.toList());
    }

    /**
     * Calculates reaction summary from a list of reactions.
     * This method can be used internally and by other services that need reaction summaries.
     */
    public MessageReactionsSummaryDto calculateReactionsSummary(List<Reaction> reactions, Integer currentUserId, Integer messageId) {
        MessageReactionsSummaryDto summary = new MessageReactionsSummaryDto();
        summary.setMessageId(messageId);
        summary.setTotalReactions(reactions.size());
        
        // Group reactions by emoji and count them
        Map<String, Long> reactionCounts = reactions.stream()
            .collect(Collectors.groupingBy(
                Reaction::getEmoji,
                Collectors.counting()
            ));
        summary.setReactionCounts(reactionCounts);
        
        // Determine if current user reacted with each emoji
        Map<String, Boolean> userReactions = reactions.stream()
            .filter(reaction -> reaction.getUser().getUserId().equals(currentUserId))
            .collect(Collectors.toMap(
                Reaction::getEmoji,
                reaction -> true,
                (existing, replacement) -> existing // merge function for duplicate keys
            ));
        summary.setUserReactions(userReactions);
        
        // Get top 3 most used reactions (for UI display)
        List<String> topReactions = reactionCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        summary.setTopReactions(topReactions);
        
        return summary;
    }

    // ========== Private Helper Methods ==========
    
    private void validateConversationMembership(com.tuniv.backend.chat.model.Conversation conversation, UserDetailsImpl user) {
        // This would typically use a service to check membership
        // For now, we'll use a simple approach - in a real implementation, 
        // you might want to inject ConversationParticipantRepository or a service
        try {
            // This is a simplified check - you might want to implement proper membership validation
            entityFinderService.getConversationParticipantOrThrow(conversation.getConversationId(), user.getId());
        } catch (Exception e) {
            throw new AccessDeniedException("You are not a member of this conversation");
        }
    }
}