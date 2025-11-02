package com.tuniv.backend.chat.mapper.mapstruct;

import java.util.List;
import java.util.Map;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.tuniv.backend.chat.dto.common.ReactionDto;
import com.tuniv.backend.chat.dto.response.MessageReactionsSummaryDto;
import com.tuniv.backend.chat.model.Reaction;
import com.tuniv.backend.chat.projection.reaction.ReactionProjection;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReactionMapper {
    
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "profilePhotoUrl", source = "user.profilePhotoUrl")
    @Mapping(target = "messageId", source = "message.id")
    ReactionDto toReactionDto(Reaction reaction);
    
    List<ReactionDto> toReactionDtoList(List<Reaction> reactions);
    
    /**
     * Direct mapping from projection for performance
     */
    default ReactionDto projectionToDto(ReactionProjection projection) {
        if (projection == null) {
            return null;
        }
        
        return new ReactionDto(
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
        );
    }
    
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