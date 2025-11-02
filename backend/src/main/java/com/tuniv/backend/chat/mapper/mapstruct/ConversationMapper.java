package com.tuniv.backend.chat.mapper.mapstruct;

import com.tuniv.backend.chat.dto.response.ConversationDetailDto;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.model.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", 
        uses = {ParticipantMapper.class, MessageMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ConversationMapper {
    
    @Mapping(target = "participants", source = "participants")
    @Mapping(target = "pinnedMessages", source = "pinnedMessages")
    @Mapping(target = "currentUserParticipant", source = "currentUserParticipant")
    ConversationDetailDto toConversationDetailDto(
        Conversation conversation,
        List<ConversationParticipant> participants,
        List<Message> pinnedMessages,
        ConversationParticipant currentUserParticipant
    );
    
    default String mapConversationType(com.tuniv.backend.chat.model.ConversationType type) {
        return type != null ? type.name() : null;
    }
}