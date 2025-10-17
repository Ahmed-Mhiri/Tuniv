package com.tuniv.backend.chat.mapper.mapstruct;

import com.tuniv.backend.chat.dto.ReactionDto;
import com.tuniv.backend.chat.dto.MessageReactionsSummaryDto;
import com.tuniv.backend.chat.model.Reaction;
import org.mapstruct.*;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReactionMapper {
    
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "profilePhotoUrl", source = "user.profilePhotoUrl")
    @Mapping(target = "messageId", source = "message.id")
    ReactionDto toReactionDto(Reaction reaction);
    
    List<ReactionDto> toReactionDtoList(List<Reaction> reactions);
    
    // Simplified method - just mapping, no business logic
    default MessageReactionsSummaryDto toMessageReactionsSummaryDto(
        Map<String, Long> reactionCounts,
        Map<String, Boolean> userReactions,
        Integer totalReactions,
        Integer messageId
    ) {
        return new MessageReactionsSummaryDto(
            messageId,
            reactionCounts,
            userReactions,
            totalReactions
        );
    }
}