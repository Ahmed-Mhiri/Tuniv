package com.tuniv.backend.chat.mapper;

import com.tuniv.backend.chat.dto.*;
import com.tuniv.backend.chat.model.*;
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.chat.repository.MessageRepository;
import com.tuniv.backend.chat.repository.ReactionRepository;
import com.tuniv.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChatMapper {

    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final ReactionRepository reactionRepository;

    // ========== Conversation Mappings ==========

    public ConversationSummaryDto toConversationSummaryDto(Conversation conversation, Integer currentUserId) {
        if (conversation == null) return null;
        
        ConversationSummaryDto dto = new ConversationSummaryDto();
        dto.setConversationId(conversation.getConversationId());
        dto.setTitle(conversation.getTitle());
        dto.setConversationType(conversation.getConversationType().name());
        dto.setLastMessageBody(conversation.getLastMessageBody());
        dto.setLastMessageSentAt(conversation.getLastMessageSentAt());
        dto.setParticipantCount(conversation.getParticipantCount());
        dto.setArchived(conversation.isArchived());
        dto.setUpdatedAt(conversation.getUpdatedAt());

        // Set last message author info
        if (conversation.getLastMessageAuthor() != null) {
            dto.setLastMessageAuthorId(conversation.getLastMessageAuthor().getUserId());
            dto.setLastMessageAuthorName(conversation.getLastMessageAuthor().getUsername());
        }

        // Calculate unread count for current user using repository
        if (currentUserId != null) {
            Integer unreadCount = participantRepository
                .findByConversationAndUser_UserId(conversation, currentUserId)
                .map(ConversationParticipant::getUnreadCount)
                .orElse(0);
            dto.setUnreadCount(unreadCount);
        }

        return dto;
    }

    public ConversationDetailDto toConversationDetailDto(Conversation conversation, User currentUser) {
        if (conversation == null) return null;
        
        ConversationDetailDto dto = new ConversationDetailDto();
        dto.setConversationId(conversation.getConversationId());
        dto.setTitle(conversation.getTitle());
        dto.setConversationType(conversation.getConversationType().name());
        dto.setMessageCount(conversation.getMessageCount());
        dto.setParticipantCount(conversation.getParticipantCount());
        dto.setActive(conversation.isActive());
        dto.setArchived(conversation.isArchived());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());

        // Set context info
        if (conversation.getUniversityContext() != null) {
            dto.setUniversityContextId(conversation.getUniversityContext().getUniversityId());
        }

        // Set last message info
        dto.setLastMessageBody(conversation.getLastMessageBody());
        dto.setLastMessageSentAt(conversation.getLastMessageSentAt());
        if (conversation.getLastMessageAuthor() != null) {
            dto.setLastMessageAuthorId(conversation.getLastMessageAuthor().getUserId());
            dto.setLastMessageAuthorName(conversation.getLastMessageAuthor().getUsername());
        }

        // Load participants using repository
        List<ParticipantDto> participants = participantRepository
            .findByConversationAndIsActiveTrue(conversation)
            .stream()
            .map(this::toParticipantDto)
            .collect(Collectors.toList());
        dto.setParticipants(participants);

        // Load pinned messages using repository
        List<PinnedMessageDto> pinnedMessages = messageRepository
            .findByConversationAndPinnedTrueAndDeletedFalse(conversation)
            .stream()
            .map(this::toPinnedMessageDto)
            .collect(Collectors.toList());
        dto.setPinnedMessages(pinnedMessages);

        // Set current user participant info
        if (currentUser != null) {
            participantRepository
                .findByConversationAndUser_UserId(conversation, currentUser.getUserId())
                .ifPresent(participant -> 
                    dto.setCurrentUserParticipant(toParticipantDto(participant))
                );
        }

        return dto;
    }

    public ParticipantDto toParticipantDto(ConversationParticipant participant) {
        if (participant == null) return null;
        
        ParticipantDto dto = new ParticipantDto();
        dto.setUserId(participant.getUser().getUserId());
        dto.setUsername(participant.getUser().getUsername());
        dto.setProfilePhotoUrl(participant.getUser().getProfilePhotoUrl());
        dto.setNickname(participant.getNickname());
        dto.setJoinedAt(participant.getJoinedAt());
        dto.setLastActiveAt(participant.getLastActiveAt());
        dto.setMessageCount(participant.getMessageCount());
        dto.setActive(participant.isActive());
        dto.setNotificationsEnabled(participant.isNotificationsEnabled());
        dto.setMuted(participant.isMuted());
        dto.setMutedUntil(participant.getMutedUntil());
        dto.setUnreadCount(participant.getUnreadCount());
        dto.setLastReadTimestamp(participant.getLastReadTimestamp());

        // Set role info
        if (participant.getRole() != null) {
            dto.setRoleId(participant.getRole().getId());
            dto.setRoleName(participant.getRole().getName());
            dto.setDisplayName(getDisplayName(participant));
        }

        return dto;
    }

    private String getDisplayName(ConversationParticipant participant) {
        return participant.getNickname() != null ? 
               participant.getNickname() : 
               participant.getUser().getUsername();
    }

    // ========== Message Mappings ==========

    // Update the existing method to call the basic one first:
public ChatMessageDto toChatMessageDto(Message message, Integer currentUserId) {
    ChatMessageDto dto = toChatMessageDto(message); // Get basic info
    
    if (currentUserId != null) {
        // Load reactions using repository
        List<ReactionDto> reactions = reactionRepository
            .findByMessageAndIsRemovedFalse(message)
            .stream()
            .map(this::toReactionDto)
            .collect(Collectors.toList());
        dto.setReactions(reactions);

        // Create reactions summary
        MessageReactionsSummaryDto summary = toMessageReactionsSummaryDto(message, currentUserId);
        dto.setReactionsSummary(summary);
    }
    
    return dto;
}

    public ReactionDto toReactionDto(Reaction reaction) {
        if (reaction == null) return null;
        
        ReactionDto dto = new ReactionDto();
        dto.setId(reaction.getId());
        dto.setMessageId(reaction.getMessage().getId());
        dto.setEmoji(reaction.getEmoji());
        dto.setSkinTone(reaction.getSkinTone());
        dto.setCustomText(reaction.getCustomText());
        dto.setCreatedAt(reaction.getCreatedAt());
        dto.setRemoved(reaction.isRemoved());
        dto.setRemovedAt(reaction.getRemovedAt());

        // Set user info
        if (reaction.getUser() != null) {
            dto.setUserId(reaction.getUser().getUserId());
            dto.setUsername(reaction.getUser().getUsername());
            dto.setProfilePhotoUrl(reaction.getUser().getProfilePhotoUrl());
        }

        return dto;
    }

    private MessageReactionsSummaryDto toMessageReactionsSummaryDto(Message message, Integer currentUserId) {
        MessageReactionsSummaryDto summary = new MessageReactionsSummaryDto();
        summary.setMessageId(message.getId());

        // Get reactions for this message
        List<Reaction> reactions = reactionRepository.findByMessageAndIsRemovedFalse(message);

        // Group by emoji and count
        var reactionCounts = reactions.stream()
            .collect(Collectors.groupingBy(
                Reaction::getEmoji,
                Collectors.counting()
            ));
        summary.setReactionCounts(reactionCounts);

        // Check if current user has reacted with each emoji
        if (currentUserId != null) {
            var userReactions = reactions.stream()
                .filter(reaction -> reaction.getUser().getUserId().equals(currentUserId))
                .collect(Collectors.toMap(
                    Reaction::getEmoji,
                    reaction -> true
                ));
            summary.setUserReactions(userReactions);
        }

        summary.setTotalReactions(reactions.size());
        return summary;
    }

    public PinnedMessageDto toPinnedMessageDto(Message message) {
        if (message == null) return null;
        
        PinnedMessageDto dto = new PinnedMessageDto();
        dto.setId(message.getId());
        dto.setBody(message.getBody());
        dto.setConversationId(message.getConversation().getConversationId());
        dto.setSentAt(message.getSentAt());

        // Set author info
        if (message.getAuthor() != null) {
            dto.setAuthorId(message.getAuthor().getUserId());
            dto.setAuthorUsername(message.getAuthor().getUsername());
        }

        // Note: pinnedBy information would need to be stored separately
        // For now, we'll leave these as null
        dto.setPinnedAt(null);
        dto.setPinnedByUserId(null);
        dto.setPinnedByUsername(null);

        return dto;
    }

    // ========== Utility Methods ==========

    public Page<ConversationSummaryDto> toConversationSummaryDtoPage(Page<Conversation> conversations, Integer currentUserId) {
        return conversations.map(conv -> toConversationSummaryDto(conv, currentUserId));
    }

    public Page<ChatMessageDto> toChatMessageDtoPage(Page<Message> messages, Integer currentUserId) {
        return messages.map(msg -> toChatMessageDto(msg, currentUserId));
    }

    public List<ChatMessageDto> toChatMessageDtoList(List<Message> messages, Integer currentUserId) {
        return messages.stream()
            .map(msg -> toChatMessageDto(msg, currentUserId))
            .collect(Collectors.toList());
    }

    private String truncateMessageBody(String body, int maxLength) {
        if (body == null) return null;
        return body.length() > maxLength ? body.substring(0, maxLength) + "..." : body;
    }

    // ========== Real-time Message Creation ==========

    public RealtimeMessage<ChatMessageDto> toRealtimeMessage(ChatMessageDto messageDto, String eventType) {
        RealtimeMessage<ChatMessageDto> realtimeMessage = new RealtimeMessage<>();
        realtimeMessage.setType(eventType);
        realtimeMessage.setData(messageDto);
        realtimeMessage.setConversationId(messageDto.getConversationId());
        realtimeMessage.setDescription("Message event: " + eventType);
        return realtimeMessage;
    }

    public RealtimeMessage<ParticipantDto> toRealtimeMessage(ParticipantDto participantDto, String eventType) {
        RealtimeMessage<ParticipantDto> realtimeMessage = new RealtimeMessage<>();
        realtimeMessage.setType(eventType);
        realtimeMessage.setData(participantDto);
        realtimeMessage.setDescription("Participant event: " + eventType);
        return realtimeMessage;
    }

public ChatMessageDto toChatMessageDto(Message message) {
    if (message == null) return null;
    
    ChatMessageDto dto = new ChatMessageDto();
    dto.setId(message.getId());
    dto.setBody(message.getBody());
    dto.setConversationId(message.getConversation().getConversationId());
    dto.setSentAt(message.getSentAt());
    dto.setEditedAt(message.getEditedAt());
    dto.setEdited(message.isEdited());
    dto.setEditCount(message.getEditCount());
    dto.setMessageType(message.getMessageType().name());
    dto.setDeleted(message.isDeleted());
    dto.setDeletedAt(message.getDeletedAt());
    dto.setDeletionReason(message.getDeletionReason());
    dto.setPinned(message.isPinned());
    dto.setClientMessageId(message.getClientMessageId());

    // Set author info
    if (message.getAuthor() != null) {
        dto.setAuthorId(message.getAuthor().getUserId());
        dto.setAuthorUsername(message.getAuthor().getUsername());
        dto.setAuthorProfilePhotoUrl(message.getAuthor().getProfilePhotoUrl());
    }

    // Set reply info
    if (message.getReplyToMessage() != null) {
        dto.setReplyToMessageId(message.getReplyToMessage().getId());
        dto.setReplyToMessageBody(truncateMessageBody(message.getReplyToMessage().getBody(), 100));
        if (message.getReplyToMessage().getAuthor() != null) {
            dto.setReplyToAuthorId(message.getReplyToMessage().getAuthor().getUserId());
            dto.setReplyToAuthorUsername(message.getReplyToMessage().getAuthor().getUsername());
        }
    }

    // For the basic version, don't load reactions
    dto.setReactions(new ArrayList<>());
    dto.setReactionsSummary(new MessageReactionsSummaryDto());
    dto.setReadByCurrentUser(false);

    return dto;
}

public List<ChatMessageDto> toChatMessageDtoListOptimized(List<Message> messages, Integer currentUserId) {
    if (messages.isEmpty()) {
        return Collections.emptyList();
    }

    // 1. Map messages to basic DTOs without reactions
    List<ChatMessageDto> dtos = messages.stream()
        .map(this::toChatMessageDto) // Use the basic mapper
        .collect(Collectors.toList());

    // 2. Fetch all reactions for these messages in one query
    List<Integer> messageIds = messages.stream().map(Message::getId).collect(Collectors.toList());
    
    // Add this method to ReactionRepository:
    // List<Reaction> findByMessage_IdInAndIsRemovedFalse(List<Integer> messageIds);
    List<Reaction> allReactions = reactionRepository.findByMessage_IdInAndIsRemovedFalse(messageIds);

    // 3. Group reactions by message ID
    Map<Integer, List<Reaction>> reactionsByMessageId = allReactions.stream()
        .collect(Collectors.groupingBy(r -> r.getMessage().getId()));

    // 4. Attach reactions to DTOs and build summaries
    for (ChatMessageDto dto : dtos) {
        List<Reaction> reactionsForMessage = reactionsByMessageId.getOrDefault(dto.getId(), Collections.emptyList());
        
        // Set reactions
        List<ReactionDto> reactionDtos = reactionsForMessage.stream()
            .map(this::toReactionDto)
            .collect(Collectors.toList());
        dto.setReactions(reactionDtos);

        // Build and set reactions summary
        MessageReactionsSummaryDto summary = toMessageReactionsSummaryDto(reactionsForMessage, currentUserId);
        summary.setMessageId(dto.getId());
        dto.setReactionsSummary(summary);
    }

    return dtos;
}

private MessageReactionsSummaryDto toMessageReactionsSummaryDto(List<Reaction> reactions, Integer currentUserId) {
    MessageReactionsSummaryDto summary = new MessageReactionsSummaryDto();

    // Group by emoji and count
    Map<String, Long> reactionCounts = reactions.stream()
        .collect(Collectors.groupingBy(
            Reaction::getEmoji,
            Collectors.counting()
        ));
    summary.setReactionCounts(reactionCounts);

    // Check if current user has reacted with each emoji
    if (currentUserId != null) {
        Map<String, Boolean> userReactions = reactions.stream()
            .filter(reaction -> reaction.getUser().getUserId().equals(currentUserId))
            .collect(Collectors.toMap(
                Reaction::getEmoji,
                reaction -> true
            ));
        summary.setUserReactions(userReactions);
    }

    summary.setTotalReactions(reactions.size());
    return summary;
}
public List<ConversationSummaryDto> toConversationSummaryDtoListOptimized(List<Conversation> conversations, Integer currentUserId) {
    if (conversations.isEmpty()) {
        return Collections.emptyList();
    }

    List<Integer> conversationIds = conversations.stream()
        .map(Conversation::getConversationId)
        .collect(Collectors.toList());

    // Fetch all relevant participant data in one query
    Map<Integer, ConversationParticipant> participantMap = participantRepository
        .findByConversationIdsAndUserId(conversationIds, currentUserId)
        .stream()
        .collect(Collectors.toMap(p -> p.getConversation().getConversationId(), p -> p));

    return conversations.stream().map(conversation -> {
        ConversationSummaryDto dto = toConversationSummaryDto(conversation, null); // Basic mapping without unread count
        ConversationParticipant participant = participantMap.get(conversation.getConversationId());
        dto.setUnreadCount(participant != null ? participant.getUnreadCount() : 0);
        return dto;
    }).collect(Collectors.toList());
}

    
}
