package com.tuniv.backend.chat.mapper.mapstruct;

import com.tuniv.backend.chat.dto.ReadReceiptDto;
import com.tuniv.backend.chat.model.ConversationParticipant;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReadReceiptMapper {
    
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "profilePhotoUrl", source = "user.profilePhotoUrl")
    @Mapping(target = "conversationId", source = "conversation.conversationId")
    @Mapping(target = "readAt", source = "lastReadTimestamp")
    ReadReceiptDto toReadReceiptDto(ConversationParticipant participant);
    
    List<ReadReceiptDto> toReadReceiptDtoList(List<ConversationParticipant> participants);
}