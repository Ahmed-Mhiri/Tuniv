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
import lombok.extern.slf4j.Slf4j;


@Component
@RequiredArgsConstructor
@Slf4j
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
        // Create reactions summary only - no detailed list
        MessageReactionsSummaryDto summary = toMessageReactionsSummaryDto(message, currentUserId);
        dto.setReactionsSummary(summary);
        
        // Remove this line that loads detailed reactions
        // dto.setReactions(reactions);
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

        if (message.getAuthor() != null) {
            dto.setAuthorId(message.getAuthor().getUserId());
            dto.setAuthorUsername(message.getAuthor().getUsername());
        }

        // ✅ FIXED: Map the new fields
        dto.setPinnedAt(message.getPinnedAt());
        if (message.getPinnedBy() != null) {
            dto.setPinnedByUserId(message.getPinnedBy().getUserId());
            dto.setPinnedByUsername(message.getPinnedBy().getUsername());
        }

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
    List<Integer> messageIds = messages.stream()
        .map(Message::getId)
        .collect(Collectors.toList());
    
    List<Reaction> allReactions = reactionRepository.findByMessage_IdInAndIsRemovedFalse(messageIds);

    // 3. Group reactions by message ID for efficient lookup
    Map<Integer, List<Reaction>> reactionsByMessageId = allReactions.stream()
        .collect(Collectors.groupingBy(r -> r.getMessage().getId()));

    // 4. Build and set reactions summaries only (no detailed lists)
    for (ChatMessageDto dto : dtos) {
        List<Reaction> reactionsForMessage = reactionsByMessageId.getOrDefault(dto.getId(), Collections.emptyList());
        
        // Build and set reactions summary only
        MessageReactionsSummaryDto summary = toMessageReactionsSummaryDto(reactionsForMessage, currentUserId);
        summary.setMessageId(dto.getId());
        dto.setReactionsSummary(summary);
        
        // Remove this line that sets detailed reactions
        // dto.setReactions(reactionDtos);
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
                    reaction -> true,
                    (existing, replacement) -> existing // Merge function for duplicate keys
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

        // Fetch all participant data in one query
        List<Integer> conversationIds = conversations.stream()
            .map(Conversation::getConversationId)
            .collect(Collectors.toList());
        
        List<ConversationParticipant> participants = participantRepository
            .findByConversation_ConversationIdInAndUser_UserIdAndIsActiveTrue(conversationIds, currentUserId);

        // Create a map for quick lookup
        Map<Integer, ConversationParticipant> participantMap = participants.stream()
            .collect(Collectors.toMap(
                cp -> cp.getConversation().getConversationId(),
                cp -> cp
            ));

        // Map to DTOs using pre-fetched data
        return conversations.stream()
            .map(conversation -> {
                ConversationSummaryDto dto = toConversationSummaryDto(conversation, null); // Don't calculate unread count in the basic method
                
                // Set unread count from pre-fetched participant data
                ConversationParticipant participant = participantMap.get(conversation.getConversationId());
                if (participant != null) {
                    dto.setUnreadCount(participant.getUnreadCount());
                } else {
                    dto.setUnreadCount(0);
                }
                
                return dto;
            })
            .collect(Collectors.toList());
    }

    public ConversationDetailDto toConversationDetailDtoOptimized(Conversation conversation, User currentUser, 
                                                                 List<ConversationParticipant> participants,
                                                                 List<Message> pinnedMessages) {
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

        // Use pre-fetched participants
        List<ParticipantDto> participantDtos = participants.stream()
            .map(this::toParticipantDto)
            .collect(Collectors.toList());
        dto.setParticipants(participantDtos);

        // Use pre-fetched pinned messages
        List<PinnedMessageDto> pinnedMessageDtos = pinnedMessages.stream()
            .map(this::toPinnedMessageDto)
            .collect(Collectors.toList());
        dto.setPinnedMessages(pinnedMessageDtos);

        // Set current user participant info
        if (currentUser != null) {
            participants.stream()
                .filter(p -> p.getUser().getUserId().equals(currentUser.getUserId()))
                .findFirst()
                .ifPresent(participant -> 
                    dto.setCurrentUserParticipant(toParticipantDto(participant))
                );
        }

        return dto;
    }

    // Add to ChatMapper.java

    public ChatMessageSummaryDto toChatMessageSummaryDto(Message message, Integer currentUserId) {
        if (message == null) return null;
        
        ChatMessageSummaryDto dto = new ChatMessageSummaryDto();
        dto.setId(message.getId());
        dto.setBody(message.getBody());
        dto.setConversationId(message.getConversation().getConversationId());
        dto.setSentAt(message.getSentAt());
        dto.setEdited(message.isEdited());
        dto.setMessageType(message.getMessageType().name());
        dto.setPinned(message.isPinned());

        // Set author info
        if (message.getAuthor() != null) {
            dto.setAuthorId(message.getAuthor().getUserId());
            dto.setAuthorUsername(message.getAuthor().getUsername());
            dto.setAuthorProfilePhotoUrl(message.getAuthor().getProfilePhotoUrl());
        }

        // Set basic reply info
        if (message.getReplyToMessage() != null) {
            dto.setReplyToMessageId(message.getReplyToMessage().getId());
            dto.setReplyToMessageBody(truncateMessageBody(message.getReplyToMessage().getBody(), 100));
        }

        // Set reactions summary (lightweight)
        if (currentUserId != null) {
            MessageReactionsSummaryDto summary = toMessageReactionsSummaryDto(message, currentUserId);
            dto.setReactionsSummary(summary);
        }

        return dto;
    }

    public List<ChatMessageSummaryDto> toChatMessageSummaryDtoList(List<Message> messages, Integer currentUserId) {
        return messages.stream()
                .map(msg -> toChatMessageSummaryDto(msg, currentUserId))
                .collect(Collectors.toList());
    }

    public Page<ChatMessageSummaryDto> toChatMessageSummaryDtoPage(Page<Message> messages, Integer currentUserId) {
        return messages.map(msg -> toChatMessageSummaryDto(msg, currentUserId));
    }

    public ChatMessageLightDto toChatMessageLightDto(Message message, Integer currentUserId) {
    if (message == null) return null;
    
    ChatMessageLightDto dto = new ChatMessageLightDto();
    dto.setId(message.getId());
    dto.setBody(message.getBody());
    dto.setConversationId(message.getConversation().getConversationId());
    dto.setSentAt(message.getSentAt());
    dto.setEdited(message.isEdited());
    dto.setMessageType(message.getMessageType().name());
    dto.setPinned(message.isPinned());

    // Set author info
    if (message.getAuthor() != null) {
        dto.setAuthorId(message.getAuthor().getUserId());
        dto.setAuthorUsername(message.getAuthor().getUsername());
    }

    // Set lightweight reactions
    if (currentUserId != null) {
        MessageReactionsSummaryDto summary = toMessageReactionsSummaryDto(message, currentUserId);
        dto.setReactionCounts(summary.getReactionCounts());
        dto.setTotalReactions(summary.getTotalReactions());
    }

    return dto;
}

    private long calculateReadByCount(Message message) {
        try {
            // Count how many participants have read this message
            // A message is considered "read" if the participant's lastReadTimestamp is after this message's sentAt
            return participantRepository.countByConversationAndLastReadTimestampAfter(
                message.getConversation(), 
                message.getSentAt()
            );
        } catch (Exception e) {
            log.warn("Failed to calculate readBy count for message {}: {}", message.getId(), e.getMessage());
            return 0;
        }
    }
public ReadReceiptDto toReadReceiptDto(ConversationParticipant participant) {
    if (participant == null) return null;
    
    ReadReceiptDto dto = new ReadReceiptDto();
    dto.setUserId(participant.getUser().getUserId());
    dto.setUsername(participant.getUser().getUsername());
    dto.setProfilePhotoUrl(participant.getUser().getProfilePhotoUrl());
    dto.setConversationId(participant.getConversation().getConversationId());
    dto.setLastReadTimestamp(participant.getLastReadTimestamp());
    dto.setReadAt(participant.getLastReadTimestamp()); // Use lastReadTimestamp as readAt
    
    return dto;
}
}