package com.tuniv.backend.chat.mapper.mapstruct;

import com.tuniv.backend.chat.dto.*;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.model.Reaction;
import org.mapstruct.*;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", 
        uses = {ReactionMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MessageMapper {
    
    // Basic message mapping
    @Mapping(target = "authorId", source = "author.userId")
    @Mapping(target = "authorUsername", source = "author.username")
    @Mapping(target = "authorProfilePhotoUrl", source = "author.profilePhotoUrl")
    @Mapping(target = "conversationId", source = "conversation.conversationId")
    @Mapping(target = "messageType", source = "messageType")
    @Mapping(target = "replyToMessageId", source = "replyToMessage.id")
    @Mapping(target = "replyToMessageBody", expression = "java(truncateMessageBody(message.getReplyToMessage() != null ? message.getReplyToMessage().getBody() : null, 100))")
    @Mapping(target = "replyToAuthorId", source = "replyToMessage.author.userId")
    @Mapping(target = "replyToAuthorUsername", source = "replyToMessage.author.username")
    @Mapping(target = "reactionsSummary", ignore = true)
    @Mapping(target = "readByCount", ignore = true)
    @Mapping(target = "isReadByCurrentUser", ignore = true)
    ChatMessageDto toChatMessageDto(Message message);
    
    // Mapping with reactions summary
    @Mapping(target = "reactionsSummary", source = "reactionsSummary")
    ChatMessageDto toChatMessageDto(Message message, MessageReactionsSummaryDto reactionsSummary);
    
    // Pinned message mapping
    @Mapping(target = "authorId", source = "author.userId")
    @Mapping(target = "authorUsername", source = "author.username")
    @Mapping(target = "conversationId", source = "conversation.conversationId")
    @Mapping(target = "pinnedByUserId", source = "pinnedBy.userId")
    @Mapping(target = "pinnedByUsername", source = "pinnedBy.username")
    PinnedMessageDto toPinnedMessageDto(Message message);
    
    // Lightweight mappings
    @Mapping(target = "authorId", source = "author.userId")
    @Mapping(target = "authorUsername", source = "author.username")
    @Mapping(target = "conversationId", source = "conversation.conversationId")
    ChatMessageSummaryDto toChatMessageSummaryDto(Message message);
    
    @Mapping(target = "authorId", source = "author.userId")
    @Mapping(target = "authorUsername", source = "author.username")
    @Mapping(target = "conversationId", source = "conversation.conversationId")
    @Mapping(target = "reactionCounts", ignore = true)
    @Mapping(target = "totalReactions", ignore = true)
    ChatMessageLightDto toChatMessageLightDto(Message message);
    
    // Bulk mappings
    List<ChatMessageDto> toChatMessageDtoList(List<Message> messages);
    List<PinnedMessageDto> toPinnedMessageDtoList(List<Message> messages);
    List<ChatMessageSummaryDto> toChatMessageSummaryDtoList(List<Message> messages);
    
    default String mapMessageType(com.tuniv.backend.chat.model.MessageType messageType) {
        return messageType != null ? messageType.name() : null;
    }
    
    default String truncateMessageBody(String body, int maxLength) {
        if (body == null) return null;
        return body.length() > maxLength ? body.substring(0, maxLength) + "..." : body;
    }
}