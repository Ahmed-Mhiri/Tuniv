package com.tuniv.backend.chat.mapper.mapstruct;

import com.tuniv.backend.chat.dto.ParticipantDto;
import com.tuniv.backend.chat.model.ConversationParticipant;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", 
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        // Add these configurations
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface ParticipantMapper {
    
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "profilePhotoUrl", source = "user.profilePhotoUrl")
    @Mapping(target = "roleId", source = "role.id")
    @Mapping(target = "roleName", source = "role.name")
    @Mapping(target = "displayName", expression = "java(getDisplayName(participant))")
    ParticipantDto toParticipantDto(ConversationParticipant participant);
    
    default String getDisplayName(ConversationParticipant participant) {
        return participant.getNickname() != null ? 
               participant.getNickname() : 
               participant.getUser().getUsername();
    }
    
    // Explicitly define the list mapping method
    List<ParticipantDto> toParticipantDtoList(List<ConversationParticipant> participants);
}