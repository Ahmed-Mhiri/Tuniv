package com.tuniv.backend.chat.mapper.mapstruct;

import com.tuniv.backend.chat.dto.*;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.user.model.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", 
        uses = {ParticipantMapper.class, MessageMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ConversationMapper {
    
    // Basic mappings
    ConversationSummaryDto toConversationSummaryDto(Conversation conversation);
    
    @Mapping(target = "unreadCount", ignore = true)
    ConversationSummaryDto toConversationSummaryDto(Conversation conversation, @Context Integer currentUserId);
    
    @Mapping(target = "participants", source = "participants")
    @Mapping(target = "pinnedMessages", source = "pinnedMessages")
    @Mapping(target = "currentUserParticipant", source = "currentUserParticipant")
    ConversationDetailDto toConversationDetailDto(
        Conversation conversation,
        List<ConversationParticipant> participants,
        List<Message> pinnedMessages,
        ConversationParticipant currentUserParticipant
    );
    
    // Bulk optimized mappings
    List<ConversationSummaryDto> toConversationSummaryDtoList(
        List<Conversation> conversations,
        @Context Integer currentUserId
    );
    
    // Helper methods for custom logic
    @AfterMapping
    default void setUnreadCount(
        @MappingTarget ConversationSummaryDto dto, 
        Conversation conversation, 
        @Context Integer currentUserId
    ) {
        // This will be handled by service layer with bulk fetching
        // The service will set unreadCount after mapping
    }
    
    default String mapConversationType(com.tuniv.backend.chat.model.ConversationType type) {
        return type != null ? type.name() : null;
    }
}