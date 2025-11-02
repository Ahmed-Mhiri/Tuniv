package com.tuniv.backend.chat.mapper.mapstruct;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.tuniv.backend.chat.dto.response.ChatMessageDto;
import com.tuniv.backend.chat.dto.response.ChatMessageLightDto;
import com.tuniv.backend.chat.dto.response.ChatMessageSummaryDto;
import com.tuniv.backend.chat.dto.response.MessageReactionsSummaryDto;
import com.tuniv.backend.chat.dto.response.PinnedMessageDto;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.projection.message.MessageListProjection;
import com.tuniv.backend.chat.projection.message.PinnedMessageProjection;

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
    @Mapping(target = "authorProfilePhotoUrl", source = "author.profilePhotoUrl")
    @Mapping(target = "conversationId", source = "conversation.conversationId")
    @Mapping(target = "pinnedByUserId", source = "pinnedBy.userId")
    @Mapping(target = "pinnedByUsername", source = "pinnedBy.username")
    @Mapping(target = "messageType", source = "messageType")
    @Mapping(target = "replyToMessageId", source = "replyToMessage.id")
    @Mapping(target = "replyToMessageBody", expression = "java(truncateMessageBody(message.getReplyToMessage() != null ? message.getReplyToMessage().getBody() : null, 100))")
    @Mapping(target = "replyToAuthorId", source = "replyToMessage.author.userId")
    @Mapping(target = "replyToAuthorUsername", source = "replyToMessage.author.username")
    @Mapping(target = "reactionsSummary", ignore = true)
    PinnedMessageDto toPinnedMessageDto(Message message);
    
    // Pinned message with reactions summary
    @Mapping(target = "authorId", source = "author.userId")
    @Mapping(target = "authorUsername", source = "author.username")
    @Mapping(target = "authorProfilePhotoUrl", source = "author.profilePhotoUrl")
    @Mapping(target = "conversationId", source = "conversation.conversationId")
    @Mapping(target = "pinnedByUserId", source = "pinnedBy.userId")
    @Mapping(target = "pinnedByUsername", source = "pinnedBy.username")
    @Mapping(target = "messageType", source = "messageType")
    @Mapping(target = "replyToMessageId", source = "replyToMessage.id")
    @Mapping(target = "replyToMessageBody", expression = "java(truncateMessageBody(message.getReplyToMessage() != null ? message.getReplyToMessage().getBody() : null, 100))")
    @Mapping(target = "replyToAuthorId", source = "replyToMessage.author.userId")
    @Mapping(target = "replyToAuthorUsername", source = "replyToMessage.author.username")
    @Mapping(target = "reactionsSummary", source = "reactionsSummary")
    PinnedMessageDto toPinnedMessageDto(Message message, MessageReactionsSummaryDto reactionsSummary);
    
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
    
    // Projection mappings
    @Mapping(target = "authorId", source = "author.userId")
    @Mapping(target = "authorUsername", source = "author.username")
    @Mapping(target = "conversationId", source = "conversation.conversationId")
    ChatMessageDto toDto(Message message);

    @Mapping(target = "id", source = "messageId")
    @Mapping(target = "body", source = "messageBody")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "messageType", source = "messageType")
    @Mapping(target = "authorId", source = "authorId")
    @Mapping(target = "authorUsername", source = "authorUsername")
    @Mapping(target = "authorProfilePhotoUrl", source = "authorProfilePhotoUrl")
    @Mapping(target = "conversationId", source = "conversationId")
    @Mapping(target = "replyToMessageId", source = "replyToMessageId")
    @Mapping(target = "replyToMessageBody", source = "replyToMessageBody")
    @Mapping(target = "replyToAuthorId", source = "replyToAuthorId")
    @Mapping(target = "replyToAuthorUsername", source = "replyToAuthorUsername")
    @Mapping(target = "isPinned", source = "pinned")
    @Mapping(target = "pinnedAt", source = "pinnedAt")
    @Mapping(target = "pinnedByUserId", source = "pinnedByUserId")
    @Mapping(target = "pinnedByUsername", source = "pinnedByUsername")
    @Mapping(target = "reactionsSummary", ignore = true)
    @Mapping(target = "readByCount", ignore = true)
    @Mapping(target = "isReadByCurrentUser", ignore = true)
    ChatMessageDto projectionToDto(MessageListProjection projection);
    
    // Pinned Message Projection mappings
    @Mapping(target = "authorProfilePhotoUrl", ignore = true) // Not in projection, ignore it
    @Mapping(target = "reactionsSummary", ignore = true) // Fetched separately
    @Mapping(target = "isPinned", constant = "true") // We know it's pinned
    PinnedMessageDto projectionToPinnedMessageDto(PinnedMessageProjection projection);

    List<PinnedMessageDto> projectionToPinnedMessageDtoList(List<PinnedMessageProjection> projections);
    
    // Bulk mappings
    List<ChatMessageDto> toChatMessageDtoList(List<Message> messages);
    List<PinnedMessageDto> toPinnedMessageDtoList(List<Message> messages);
    List<ChatMessageSummaryDto> toChatMessageSummaryDtoList(List<Message> messages);
    List<ChatMessageDto> projectionToDtoList(List<MessageListProjection> projections);
    
    default String mapMessageType(com.tuniv.backend.chat.model.MessageType messageType) {
        return messageType != null ? messageType.name() : null;
    }
    
    default String truncateMessageBody(String body, int maxLength) {
        if (body == null) return null;
        return body.length() > maxLength ? body.substring(0, maxLength) + "..." : body;
    }
}