package com.tuniv.backend.chat.mapper.mapstruct;

import com.tuniv.backend.chat.dto.response.ParticipantDto;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.projection.participant.ParticipantProjection;
import org.mapstruct.*;

import java.util.List;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface ParticipantMapper {
    
    // Entity to DTO mapping
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "profilePhotoUrl", source = "user.profilePhotoUrl")
    @Mapping(target = "roleId", source = "role.id")
    @Mapping(target = "roleName", source = "role.name")
    @Mapping(target = "displayName", expression = "java(getDisplayName(participant))")
    ParticipantDto toParticipantDto(ConversationParticipant participant);
    
    // Projection to DTO mapping
    @Mapping(target = "displayName", expression = "java(getDisplayNameFromProjection(projection))")
    ParticipantDto projectionToParticipantDto(ParticipantProjection projection);
    
    // List mappings
    List<ParticipantDto> toParticipantDtoList(List<ConversationParticipant> participants);
    List<ParticipantDto> projectionListToParticipantDtoList(List<ParticipantProjection> projections);
    
    // Helper methods
    default String getDisplayName(ConversationParticipant participant) {
        return participant.getNickname() != null ? 
               participant.getNickname() : 
               participant.getUser().getUsername();
    }
    
    default String getDisplayNameFromProjection(ParticipantProjection projection) {
        return projection.getNickname() != null ? 
               projection.getNickname() : 
               projection.getUsername();
    }
}