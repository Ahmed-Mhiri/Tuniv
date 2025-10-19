package com.tuniv.backend.chat.mapper.mapstruct;

import java.util.List;
import java.util.Map;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.tuniv.backend.chat.dto.MessageReactionsSummaryDto;
import com.tuniv.backend.chat.dto.ReactionDto;
import com.tuniv.backend.chat.model.Reaction;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReactionMapper {
    
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "profilePhotoUrl", source = "user.profilePhotoUrl")
    @Mapping(target = "messageId", source = "message.id")
    ReactionDto toReactionDto(Reaction reaction);
    
    List<ReactionDto> toReactionDtoList(List<Reaction> reactions);
    
    /**
     * Simplified method - just mapping, no business logic
     * Updated to include topReactions parameter
     */
    default MessageReactionsSummaryDto toMessageReactionsSummaryDto(
        Map<String, Long> reactionCounts,
        Map<String, Boolean> userReactions,
        Integer totalReactions,
        Integer messageId,
        List<String> topReactions
    ) {
        MessageReactionsSummaryDto summary = new MessageReactionsSummaryDto();
        summary.setMessageId(messageId);
        summary.setReactionCounts(reactionCounts);
        summary.setUserReactions(userReactions);
        summary.setTotalReactions(totalReactions);
        summary.setTopReactions(topReactions);
        return summary;
    }
    
    /**
     * Overloaded method without topReactions for backward compatibility
     */
    default MessageReactionsSummaryDto toMessageReactionsSummaryDto(
        Map<String, Long> reactionCounts,
        Map<String, Boolean> userReactions,
        Integer totalReactions,
        Integer messageId
    ) {
        return toMessageReactionsSummaryDto(reactionCounts, userReactions, totalReactions, messageId, null);
    }
}