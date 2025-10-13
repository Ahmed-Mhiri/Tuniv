package com.tuniv.backend.chat.service.impl;

import com.tuniv.backend.chat.dto.*;
import com.tuniv.backend.chat.service.ChatRealtimeService;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.chat.repository.ConversationRepository;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.tuniv.backend.user.model.User;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatRealtimeServiceImpl implements ChatRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private final Map<Integer, Set<Integer>> typingUsersByConversation = new ConcurrentHashMap<>();

    @Override
    public void broadcastNewMessage(Integer conversationId, ChatMessageDto messageDto) {
        log.debug("Broadcasting new message to conversation {}: {}", conversationId, messageDto.getId());
        
        String destination = getConversationTopic(conversationId);
        RealtimeMessage<ChatMessageDto> realtimeMessage = createRealtimeMessage(
            "MESSAGE_NEW", messageDto, "New message received"
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        
        broadcastToUserQueues(conversationId, "MESSAGE_NEW", messageDto);
        
        log.debug("New message broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastMessageUpdate(Integer conversationId, ChatMessageDto updatedMessageDto) {
        log.debug("Broadcasting message update to conversation {}: {}", conversationId, updatedMessageDto.getId());
        
        String destination = getConversationTopic(conversationId);
        RealtimeMessage<ChatMessageDto> realtimeMessage = createRealtimeMessage(
            "MESSAGE_UPDATED", updatedMessageDto, "Message updated"
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        log.debug("Message update broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastMessageDeletion(Integer conversationId, Integer messageId) {
        log.debug("Broadcasting message deletion to conversation {}: {}", conversationId, messageId);
        
        String destination = getConversationTopic(conversationId);
        MessageDeletionDto deletionDto = new MessageDeletionDto(messageId, conversationId, Instant.now());
        
        RealtimeMessage<MessageDeletionDto> realtimeMessage = createRealtimeMessage(
            "MESSAGE_DELETED", deletionDto, "Message deleted"
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        log.debug("Message deletion broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastMessagesDeletion(Integer conversationId, List<Integer> messageIds) {
        log.debug("Broadcasting bulk message deletion to conversation {}: {} messages", 
                 conversationId, messageIds.size());
        
        String destination = getConversationTopic(conversationId);
        BulkMessageDeletionDto deletionDto = new BulkMessageDeletionDto(messageIds, conversationId, Instant.now());
        
        RealtimeMessage<BulkMessageDeletionDto> realtimeMessage = createRealtimeMessage(
            "MESSAGES_DELETED", deletionDto, "Messages deleted"
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        log.debug("Bulk message deletion broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastReactionUpdate(Integer conversationId, MessageReactionUpdateDto reactionUpdateDto) {
        log.debug("Broadcasting reaction update to conversation {} for message {}", 
                 conversationId, reactionUpdateDto.getMessageId());
        
        String destination = getConversationTopic(conversationId);
        RealtimeMessage<MessageReactionUpdateDto> realtimeMessage = createRealtimeMessage(
            "REACTION_UPDATED", reactionUpdateDto, "Reaction updated"
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        log.debug("Reaction update broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastReactionsUpdate(Integer conversationId, Integer messageId, List<ReactionDto> reactions) {
        log.debug("Broadcasting reactions update to conversation {} for message {}", conversationId, messageId);
        
        String destination = getConversationTopic(conversationId);
        MessageReactionsDto reactionsDto = new MessageReactionsDto(messageId, reactions);
        
        RealtimeMessage<MessageReactionsDto> realtimeMessage = createRealtimeMessage(
            "REACTIONS_UPDATED", reactionsDto, "Reactions updated"
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        log.debug("Reactions update broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastTypingIndicator(Integer conversationId, TypingIndicatorDto typingIndicator) {
        log.debug("Broadcasting typing indicator to conversation {} from user {}", 
                 conversationId, typingIndicator.getUserId());
        
        typingUsersByConversation
            .computeIfAbsent(conversationId, k -> new CopyOnWriteArraySet<>())
            .add(typingIndicator.getUserId());
        
        String destination = getConversationTopic(conversationId);
        RealtimeMessage<TypingIndicatorDto> realtimeMessage = createRealtimeMessage(
            "USER_TYPING", typingIndicator, "User is typing"
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        log.debug("Typing indicator broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastStopTyping(Integer conversationId, Integer userId) {
        log.debug("Broadcasting stop typing to conversation {} from user {}", conversationId, userId);
        
        Optional.ofNullable(typingUsersByConversation.get(conversationId))
                .ifPresent(typingUsers -> typingUsers.remove(userId));
        
        String destination = getConversationTopic(conversationId);
        StopTypingDto stopTypingDto = new StopTypingDto(userId, conversationId, Instant.now());
        
        RealtimeMessage<StopTypingDto> realtimeMessage = createRealtimeMessage(
            "USER_STOPPED_TYPING", stopTypingDto, "User stopped typing"
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        log.debug("Stop typing broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastReadReceipt(Integer conversationId, ReadReceiptDto readReceipt) {
        log.debug("Broadcasting read receipt to conversation {} from user {}", 
                 conversationId, readReceipt.getUserId());
        
        String destination = getConversationTopic(conversationId);
        RealtimeMessage<ReadReceiptDto> realtimeMessage = createRealtimeMessage(
            "MESSAGE_READ", readReceipt, "Message read receipt"
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        log.debug("Read receipt broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastReadReceipts(Integer conversationId, List<ReadReceiptDto> readReceipts) {
        log.debug("Broadcasting {} read receipts to conversation {}", readReceipts.size(), conversationId);
        
        String destination = getConversationTopic(conversationId);
        BulkReadReceiptsDto bulkReceipts = new BulkReadReceiptsDto(readReceipts, conversationId);
        
        RealtimeMessage<BulkReadReceiptsDto> realtimeMessage = createRealtimeMessage(
            "MESSAGES_READ", bulkReceipts, "Multiple messages read"
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        log.debug("Bulk read receipts broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastParticipantJoined(Integer conversationId, ParticipantDto participantDto) {
        log.debug("Broadcasting participant joined to conversation {}: user {}", 
                 conversationId, participantDto.getUserId());
        
        String destination = getConversationTopic(conversationId);
        RealtimeMessage<ParticipantDto> realtimeMessage = createRealtimeMessage(
            "PARTICIPANT_JOINED", participantDto, "Participant joined conversation"
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        log.debug("Participant joined broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastParticipantLeft(Integer conversationId, Integer userId, String username) {
        log.debug("Broadcasting participant left to conversation {}: user {}", conversationId, userId);
        
        String destination = getConversationTopic(conversationId);
        ParticipantLeftDto leftDto = new ParticipantLeftDto(userId, username, conversationId, Instant.now());
        
        RealtimeMessage<ParticipantLeftDto> realtimeMessage = createRealtimeMessage(
            "PARTICIPANT_LEFT", leftDto, "Participant left conversation"
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        
        removeActiveUser(conversationId, userId);
        
        log.debug("Participant left broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastParticipantRoleUpdate(Integer conversationId, ParticipantDto participantDto) {
        log.debug("Broadcasting participant role update to conversation {}: user {}", 
                 conversationId, participantDto.getUserId());
        
        String destination = getConversationTopic(conversationId);
        RealtimeMessage<ParticipantDto> realtimeMessage = createRealtimeMessage(
            "PARTICIPANT_ROLE_UPDATED", participantDto, "Participant role updated"
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        log.debug("Participant role update broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastParticipantMuteUpdate(Integer conversationId, ParticipantDto participantDto) {
        log.debug("Broadcasting participant mute update to conversation {}: user {}", 
                 conversationId, participantDto.getUserId());
        
        String destination = getConversationTopic(conversationId);
        RealtimeMessage<ParticipantDto> realtimeMessage = createRealtimeMessage(
            "PARTICIPANT_MUTE_UPDATED", participantDto, "Participant mute status updated"
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        log.debug("Participant mute update broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastParticipantBanUpdate(Integer conversationId, BannedUserDto bannedUserDto, boolean isBanned) {
        log.debug("Broadcasting participant ban update to conversation {}: user {} (banned: {})", 
                 conversationId, bannedUserDto.getUserId(), isBanned);
        
        String destination = getConversationTopic(conversationId);
        String eventType = isBanned ? "PARTICIPANT_BANNED" : "PARTICIPANT_UNBANNED";
        String message = isBanned ? "Participant banned" : "Participant unbanned";
        
        RealtimeMessage<BannedUserDto> realtimeMessage = createRealtimeMessage(
            eventType, bannedUserDto, message
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        
        if (isBanned) {
            removeActiveUser(conversationId, bannedUserDto.getUserId());
        }
        
        log.debug("Participant ban update broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastConversationInfoUpdate(Integer conversationId, ConversationDetailDto conversationDto) {
        log.debug("Broadcasting conversation info update to conversation {}", conversationId);
        
        String destination = getConversationTopic(conversationId);
        RealtimeMessage<ConversationDetailDto> realtimeMessage = createRealtimeMessage(
            "CONVERSATION_UPDATED", conversationDto, "Conversation information updated"
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        log.debug("Conversation info update broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastConversationArchiveUpdate(Integer conversationId, boolean isArchived) {
        log.debug("Broadcasting conversation archive update to conversation {}: archived={}", 
                 conversationId, isArchived);
        
        String destination = getConversationTopic(conversationId);
        String eventType = isArchived ? "CONVERSATION_ARCHIVED" : "CONVERSATION_RESTORED";
        String message = isArchived ? "Conversation archived" : "Conversation restored";
        
        ConversationArchiveDto archiveDto = new ConversationArchiveDto(conversationId, isArchived, Instant.now());
        
        RealtimeMessage<ConversationArchiveDto> realtimeMessage = createRealtimeMessage(
            eventType, archiveDto, message
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        log.debug("Conversation archive update broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastConversationDeletion(Integer conversationId) {
        log.debug("Broadcasting conversation deletion to conversation {}", conversationId);
        
        String destination = getConversationTopic(conversationId);
        ConversationDeletionDto deletionDto = new ConversationDeletionDto(conversationId, Instant.now());
        
        RealtimeMessage<ConversationDeletionDto> realtimeMessage = createRealtimeMessage(
            "CONVERSATION_DELETED", deletionDto, "Conversation deleted"
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        
        cleanupConversation(conversationId);
        
        log.debug("Conversation deletion broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastUserPresence(Integer conversationId, Integer userId, boolean isOnline) {
        log.debug("Broadcasting user presence to conversation {}: user {} online={}",
                 conversationId, userId, isOnline);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String destination = getConversationTopic(conversationId);
        UserPresenceDto presenceDto = new UserPresenceDto(
                userId,
                user.getUsername(),
                conversationId,
                isOnline,
                Instant.now()
        );

        RealtimeMessage<UserPresenceDto> realtimeMessage = createRealtimeMessage(
            "USER_PRESENCE", presenceDto, "User presence updated"
        );

        messagingTemplate.convertAndSend(destination, realtimeMessage);
        log.debug("User presence broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastUserActiveStatus(Integer conversationId, Integer userId, boolean isActive) {
        log.debug("Broadcasting user active status to conversation {}: user {} active={}", 
                 conversationId, userId, isActive);
        
        String destination = getConversationTopic(conversationId);
        UserActiveStatusDto activeStatusDto = new UserActiveStatusDto(userId, conversationId, isActive, Instant.now());
        
        RealtimeMessage<UserActiveStatusDto> realtimeMessage = createRealtimeMessage(
            "USER_ACTIVE_STATUS", activeStatusDto, "User active status updated"
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        log.debug("User active status broadcast completed for conversation {}", conversationId);
    }

    @Override
    public void broadcastSystemMessage(Integer conversationId, SystemMessageDto systemMessage) {
        log.debug("Broadcasting system message to conversation {}: {}", conversationId, systemMessage.getType());
        
        String destination = getConversationTopic(conversationId);
        RealtimeMessage<SystemMessageDto> realtimeMessage = createRealtimeMessage(
            "SYSTEM_MESSAGE", systemMessage, "System message"
        );
        
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        log.debug("System message broadcast completed for conversation {}", conversationId);
    }

    @Override
    public List<Integer> getActiveUsersInConversation(Integer conversationId) {
        String key = String.format("conversation:active:users:%d", conversationId);
        Set<Object> userIds = redisTemplate.opsForSet().members(key);
        List<Integer> result = new ArrayList<>();
        
        if (userIds != null) {
            for (Object userId : userIds) {
                try {
                    result.add(Integer.parseInt(userId.toString()));
                } catch (NumberFormatException e) {
                    log.warn("Invalid user ID: {}", userId);
                }
            }
        }
        
        return result;
    }

    @Override
    public boolean isUserActiveInConversation(Integer conversationId, Integer userId) {
        List<Integer> activeUsers = getActiveUsersInConversation(conversationId);
        return activeUsers.contains(userId);
    }

    @Override
    public void notifyNewConversation(Integer conversationId, List<Integer> userIds) {
        log.debug("Notifying {} users about new conversation {}", userIds.size(), conversationId);
        
        Conversation conversation = getConversationEntity(conversationId);
        
        ConversationNotificationDto notification = new ConversationNotificationDto(
            conversation.getConversationId(),
            conversation.getTitle(),
            conversation.getConversationType().name(),
            conversation.getParticipantCount()
        );
        
        for (Integer userId : userIds) {
            String userDestination = getUserQueue(userId);
            RealtimeMessage<ConversationNotificationDto> realtimeMessage = createRealtimeMessage(
                "NEW_CONVERSATION", notification, "You've been added to a new conversation"
            );
            
            messagingTemplate.convertAndSend(userDestination, realtimeMessage);
        }
        
        log.debug("New conversation notifications sent to {} users", userIds.size());
    }

    @Override
    public void sendDirectNotification(Integer userId, ChatNotificationDto notification) {
        log.debug("Sending direct notification to user {}: {}", userId, notification.getType());
        
        String userDestination = getUserQueue(userId);
        RealtimeMessage<ChatNotificationDto> realtimeMessage = createRealtimeMessage(
            "DIRECT_NOTIFICATION", notification, "Direct notification"
        );
        
        messagingTemplate.convertAndSend(userDestination, realtimeMessage);
        log.debug("Direct notification sent to user {}", userId);
    }

    private String getConversationTopic(Integer conversationId) {
        return "/topic/conversations/" + conversationId;
    }

    private String getUserQueue(Integer userId) {
        return "/queue/user/" + userId + "/notifications";
    }

    private <T> RealtimeMessage<T> createRealtimeMessage(String type, T data, String description) {
        RealtimeMessage<T> message = new RealtimeMessage<>();
        message.setType(type);
        message.setData(data);
        message.setTimestamp(Instant.now());
        message.setDescription(description);
        return message;
    }

    private void addActiveUser(Integer conversationId, Integer userId) {
        String userConversationsKey = String.format("user:active:conversations:%d", userId);
        String conversationUsersKey = String.format("conversation:active:users:%d", conversationId);
        
        redisTemplate.opsForSet().add(userConversationsKey, conversationId.toString());
        redisTemplate.opsForSet().add(conversationUsersKey, userId.toString());
        
        log.debug("User {} marked as active in conversation {}", userId, conversationId);
    }

    private void removeActiveUser(Integer conversationId, Integer userId) {
        String userConversationsKey = String.format("user:active:conversations:%d", userId);
        String conversationUsersKey = String.format("conversation:active:users:%d", conversationId);
        
        redisTemplate.opsForSet().remove(userConversationsKey, conversationId.toString());
        redisTemplate.opsForSet().remove(conversationUsersKey, userId.toString());
        
        Optional.ofNullable(typingUsersByConversation.get(conversationId))
                .ifPresent(typingUsers -> typingUsers.remove(userId));
        
        log.debug("User {} removed from active users in conversation {}", userId, conversationId);
    }

    private void cleanupConversation(Integer conversationId) {
        String conversationUsersKey = String.format("conversation:active:users:%d", conversationId);
        Set<Object> userIds = redisTemplate.opsForSet().members(conversationUsersKey);
        
        if (userIds != null) {
            for (Object userIdObj : userIds) {
                try {
                    Integer userId = Integer.parseInt(userIdObj.toString());
                    String userConversationsKey = String.format("user:active:conversations:%d", userId);
                    redisTemplate.opsForSet().remove(userConversationsKey, conversationId.toString());
                } catch (NumberFormatException e) {
                    log.warn("Invalid user ID: {}", userIdObj);
                }
            }
        }
        
        redisTemplate.delete(conversationUsersKey);
        typingUsersByConversation.remove(conversationId);
        
        log.debug("Cleaned up realtime tracking for conversation {}", conversationId);
    }

    private void broadcastToUserQueues(Integer conversationId, String eventType, Object data) {
        List<ConversationParticipant> participants = participantRepository
                .findByConversation_ConversationIdAndIsActiveTrue(conversationId);
        
        for (ConversationParticipant participant : participants) {
            Integer userId = participant.getUser().getUserId();
            String userDestination = getUserQueue(userId);
            
            RealtimeMessage<Object> realtimeMessage = createRealtimeMessage(
                eventType, data, "User-specific notification"
            );
            
            messagingTemplate.convertAndSend(userDestination, realtimeMessage);
        }
    }

    private Conversation getConversationEntity(Integer conversationId) {
        return conversationRepository.findById(conversationId)
        .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));
    }

    private void cleanupUserPresence(Integer userId) {
        String userConversationsKey = String.format("user:active:conversations:%d", userId);
        Set<Object> conversationIds = redisTemplate.opsForSet().members(userConversationsKey);
        
        if (conversationIds != null) {
            for (Object convIdObj : conversationIds) {
                try {
                    Integer conversationId = Integer.parseInt(convIdObj.toString());
                    removeActiveUser(conversationId, userId);
                    broadcastUserPresence(conversationId, userId, false);
                } catch (NumberFormatException e) {
                    log.warn("Invalid conversation ID: {}", convIdObj);
                }
            }
        }
        
        redisTemplate.delete(userConversationsKey);
        log.debug("Completely cleaned up presence for user {}", userId);
    }
}