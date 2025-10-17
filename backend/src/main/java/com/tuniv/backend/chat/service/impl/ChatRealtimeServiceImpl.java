package com.tuniv.backend.chat.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.chat.dto.BannedUserDto;
import com.tuniv.backend.chat.dto.BulkMessageDeletionDto;
import com.tuniv.backend.chat.dto.BulkReadReceiptsDto;
import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.dto.ChatNotificationDto;
import com.tuniv.backend.chat.dto.ConversationArchiveDto;
import com.tuniv.backend.chat.dto.ConversationDeletionDto;
import com.tuniv.backend.chat.dto.ConversationDetailDto;
import com.tuniv.backend.chat.dto.ConversationNotificationDto;
import com.tuniv.backend.chat.dto.MessageDeletionDto;
import com.tuniv.backend.chat.dto.MessageReactionUpdateDto;
import com.tuniv.backend.chat.dto.MessageReactionsDto;
import com.tuniv.backend.chat.dto.ParticipantDto;
import com.tuniv.backend.chat.dto.ParticipantLeftDto;
import com.tuniv.backend.chat.dto.ReactionDto;
import com.tuniv.backend.chat.dto.ReadReceiptDto;
import com.tuniv.backend.chat.dto.RealtimeMessage;
import com.tuniv.backend.chat.dto.StopTypingDto;
import com.tuniv.backend.chat.dto.SystemMessageDto;
import com.tuniv.backend.chat.dto.TypingIndicatorDto;
import com.tuniv.backend.chat.dto.UserActiveStatusDto;
import com.tuniv.backend.chat.dto.UserPresenceDto;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.chat.repository.ConversationRepository;
import com.tuniv.backend.chat.service.ChatRealtimeService;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    private static final String TYPING_USERS_KEY = "typing:conversation:%d";
    private static final Duration TYPING_TTL = Duration.ofSeconds(10);

    // ========== Message Broadcasting ==========
    
    @Override
    public void broadcastNewMessage(Integer conversationId, ChatMessageDto messageDto) {
        log.debug("Broadcasting new message to conversation {}: {}", conversationId, messageDto.getId());
        broadcastToConversation(conversationId, "MESSAGE_NEW", messageDto, "New message received");
        broadcastToUserQueues(conversationId, "MESSAGE_NEW", messageDto);
    }
    
    @Override
    public void broadcastMessageUpdate(Integer conversationId, ChatMessageDto updatedMessageDto) {
        log.debug("Broadcasting message update to conversation {}: {}", conversationId, updatedMessageDto.getId());
        broadcastToConversation(conversationId, "MESSAGE_UPDATED", updatedMessageDto, "Message updated");
    }
    
    @Override
    public void broadcastMessageDeletion(Integer conversationId, Integer messageId) {
        log.debug("Broadcasting message deletion to conversation {}: {}", conversationId, messageId);
        MessageDeletionDto deletionDto = new MessageDeletionDto(messageId, conversationId, Instant.now());
        broadcastToConversation(conversationId, "MESSAGE_DELETED", deletionDto, "Message deleted");
    }
    
    @Override
    public void broadcastMessagesDeletion(Integer conversationId, List<Integer> messageIds) {
        log.debug("Broadcasting bulk message deletion to conversation {}: {} messages", 
                 conversationId, messageIds.size());
        BulkMessageDeletionDto deletionDto = new BulkMessageDeletionDto(messageIds, conversationId, Instant.now());
        broadcastToConversation(conversationId, "MESSAGES_DELETED", deletionDto, "Messages deleted");
    }

    // ========== Reaction Broadcasting ==========
    
    @Override
    public void broadcastReactionUpdate(Integer conversationId, MessageReactionUpdateDto reactionUpdateDto) {
        log.debug("Broadcasting reaction update to conversation {} for message {}", 
                 conversationId, reactionUpdateDto.getMessageId());
        broadcastToConversation(conversationId, "REACTION_UPDATED", reactionUpdateDto, "Reaction updated");
    }
    
    @Override
    public void broadcastReactionsUpdate(Integer conversationId, Integer messageId, List<ReactionDto> reactions) {
        log.debug("Broadcasting reactions update to conversation {} for message {}", conversationId, messageId);
        MessageReactionsDto reactionsDto = new MessageReactionsDto(messageId, reactions);
        broadcastToConversation(conversationId, "REACTIONS_UPDATED", reactionsDto, "Reactions updated");
    }

    // ========== Typing Indicators ==========
    
    @Override
    public void broadcastTypingIndicator(Integer conversationId, TypingIndicatorDto typingIndicator) {
        log.debug("Broadcasting typing indicator to conversation {} from user {}", 
                 conversationId, typingIndicator.getUserId());
        
        String typingKey = String.format(TYPING_USERS_KEY, conversationId);
        redisTemplate.opsForSet().add(typingKey, typingIndicator.getUserId().toString());
        redisTemplate.expire(typingKey, TYPING_TTL);
        
        broadcastToConversation(conversationId, "USER_TYPING", typingIndicator, "User is typing");
    }
    
    @Override
    public void broadcastStopTyping(Integer conversationId, Integer userId) {
        log.debug("Broadcasting stop typing to conversation {} from user {}", conversationId, userId);
        
        String typingKey = String.format(TYPING_USERS_KEY, conversationId);
        redisTemplate.opsForSet().remove(typingKey, userId.toString());
        
        StopTypingDto stopTypingDto = new StopTypingDto(userId, conversationId, Instant.now());
        broadcastToConversation(conversationId, "USER_STOPPED_TYPING", stopTypingDto, "User stopped typing");
    }

    // ========== Read Receipts ==========
    
    @Override
    public void broadcastReadReceipt(Integer conversationId, ReadReceiptDto readReceipt) {
        log.debug("Broadcasting read receipt to conversation {} from user {}", 
                 conversationId, readReceipt.getUserId());
        broadcastToConversation(conversationId, "MESSAGE_READ", readReceipt, "Message read receipt");
    }
    
    @Override
    public void broadcastReadReceipts(Integer conversationId, List<ReadReceiptDto> readReceipts) {
        log.debug("Broadcasting {} read receipts to conversation {}", readReceipts.size(), conversationId);
        BulkReadReceiptsDto bulkReceipts = new BulkReadReceiptsDto(readReceipts, conversationId);
        broadcastToConversation(conversationId, "MESSAGES_READ", bulkReceipts, "Multiple messages read");
    }

    // ========== Participant Updates ==========
    
    @Override
    public void broadcastParticipantJoined(Integer conversationId, ParticipantDto participantDto) {
        log.debug("Broadcasting participant joined to conversation {}: user {}", 
                 conversationId, participantDto.getUserId());
        broadcastToConversation(conversationId, "PARTICIPANT_JOINED", participantDto, "Participant joined conversation");
        
        // Mark user as active in the conversation
        addActiveUser(conversationId, participantDto.getUserId());
    }
    
    @Override
    public void broadcastParticipantLeft(Integer conversationId, Integer userId, String username) {
        log.debug("Broadcasting participant left to conversation {}: user {}", conversationId, userId);
        
        ParticipantLeftDto leftDto = new ParticipantLeftDto(userId, username, conversationId, Instant.now());
        broadcastToConversation(conversationId, "PARTICIPANT_LEFT", leftDto, "Participant left conversation");
        
        removeActiveUser(conversationId, userId);
    }
    
    @Override
    public void broadcastParticipantRoleUpdate(Integer conversationId, ParticipantDto participantDto) {
        log.debug("Broadcasting participant role update to conversation {}: user {}", 
                 conversationId, participantDto.getUserId());
        broadcastToConversation(conversationId, "PARTICIPANT_ROLE_UPDATED", participantDto, "Participant role updated");
    }
    
    @Override
    public void broadcastParticipantMuteUpdate(Integer conversationId, ParticipantDto participantDto) {
        log.debug("Broadcasting participant mute update to conversation {}: user {}", 
                 conversationId, participantDto.getUserId());
        broadcastToConversation(conversationId, "PARTICIPANT_MUTE_UPDATED", participantDto, "Participant mute status updated");
    }
    
    @Override
    public void broadcastParticipantBanUpdate(Integer conversationId, BannedUserDto bannedUserDto, boolean isBanned) {
        log.debug("Broadcasting participant ban update to conversation {}: user {} (banned: {})", 
                 conversationId, bannedUserDto.getUserId(), isBanned);
        
        String eventType = isBanned ? "PARTICIPANT_BANNED" : "PARTICIPANT_UNBANNED";
        String message = isBanned ? "Participant banned" : "Participant unbanned";
        
        broadcastToConversation(conversationId, eventType, bannedUserDto, message);
        
        if (isBanned) {
            removeActiveUser(conversationId, bannedUserDto.getUserId());
        }
    }

    // ========== Conversation Updates ==========
    
    @Override
    public void broadcastConversationInfoUpdate(Integer conversationId, ConversationDetailDto conversationDto) {
        log.debug("Broadcasting conversation info update to conversation {}", conversationId);
        broadcastToConversation(conversationId, "CONVERSATION_UPDATED", conversationDto, "Conversation information updated");
    }
    
    @Override
    public void broadcastConversationArchiveUpdate(Integer conversationId, boolean isArchived) {
        log.debug("Broadcasting conversation archive update to conversation {}: archived={}", 
                 conversationId, isArchived);
        
        String eventType = isArchived ? "CONVERSATION_ARCHIVED" : "CONVERSATION_RESTORED";
        String message = isArchived ? "Conversation archived" : "Conversation restored";
        
        ConversationArchiveDto archiveDto = new ConversationArchiveDto(conversationId, isArchived, Instant.now());
        broadcastToConversation(conversationId, eventType, archiveDto, message);
    }
    
    @Override
    public void broadcastConversationDeletion(Integer conversationId) {
        log.debug("Broadcasting conversation deletion to conversation {}", conversationId);
        
        ConversationDeletionDto deletionDto = new ConversationDeletionDto(conversationId, Instant.now());
        broadcastToConversation(conversationId, "CONVERSATION_DELETED", deletionDto, "Conversation deleted");
        
        cleanupConversation(conversationId);
    }

    // ========== Presence Updates ==========
    
    @Override
    public void broadcastUserPresence(Integer conversationId, Integer userId, boolean isOnline) {
        log.debug("Broadcasting user presence to conversation {}: user {} online={}",
                 conversationId, userId, isOnline);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        UserPresenceDto presenceDto = new UserPresenceDto(
                userId,
                user.getUsername(),
                conversationId,
                isOnline,
                Instant.now()
        );

        broadcastToConversation(conversationId, "USER_PRESENCE", presenceDto, "User presence updated");
        
        // Update active user tracking
        if (isOnline) {
            addActiveUser(conversationId, userId);
        } else {
            removeActiveUser(conversationId, userId);
        }
    }
    
    @Override
    public void broadcastUserActiveStatus(Integer conversationId, Integer userId, boolean isActive) {
        log.debug("Broadcasting user active status to conversation {}: user {} active={}", 
                 conversationId, userId, isActive);
        
        UserActiveStatusDto activeStatusDto = new UserActiveStatusDto(userId, conversationId, isActive, Instant.now());
        broadcastToConversation(conversationId, "USER_ACTIVE_STATUS", activeStatusDto, "User active status updated");
        
        // Update active user tracking
        if (isActive) {
            addActiveUser(conversationId, userId);
        } else {
            removeActiveUser(conversationId, userId);
        }
    }

    // ========== System Messages ==========
    
    @Override
    public void broadcastSystemMessage(Integer conversationId, SystemMessageDto systemMessage) {
        log.debug("Broadcasting system message to conversation {}: {}", conversationId, systemMessage.getType());
        broadcastToConversation(conversationId, "SYSTEM_MESSAGE", systemMessage, "System message");
    }

    // ========== Utility Methods ==========
    
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
            broadcastToUserQueue(userId, "NEW_CONVERSATION", notification, "You've been added to a new conversation");
        }
        
        log.debug("New conversation notifications sent to {} users", userIds.size());
    }
    
    @Override
    public void sendDirectNotification(Integer userId, ChatNotificationDto notification) {
        log.debug("Sending direct notification to user {}: {}", userId, notification.getType());
        broadcastToUserQueue(userId, "DIRECT_NOTIFICATION", notification, "Direct notification");
    }

    // ========== Additional Utility Methods ==========
    
    /**
     * Gets the set of currently typing users in a conversation
     */
    public Set<String> getTypingUsers(Integer conversationId) {
        String typingKey = String.format(TYPING_USERS_KEY, conversationId);
        Set<Object> typingUsers = redisTemplate.opsForSet().members(typingKey);
        
        if (typingUsers == null) {
            return Collections.emptySet();
        }
        
        Set<String> result = new HashSet<>();
        for (Object userId : typingUsers) {
            if (userId != null) {
                result.add(userId.toString());
            }
        }
        return result;
    }

    /**
     * Cleans up expired typing indicators (primarily handled by Redis TTL)
     */
    public void cleanupExpiredTypingIndicators() {
        log.debug("Typing indicators cleanup: Redis TTL handles automatic expiration");
    }

    // ========== Private Helper Methods ==========

    /**
     * Helper method to broadcast to conversation topic
     */
    private <T> void broadcastToConversation(Integer conversationId, String eventType, T payload, String description) {
        String destination = getConversationTopic(conversationId);
        RealtimeMessage<T> realtimeMessage = createRealtimeMessage(eventType, payload, description);
        messagingTemplate.convertAndSend(destination, realtimeMessage);
        log.debug("{} broadcast completed for conversation {}", eventType, conversationId);
    }

    /**
     * Helper method to send to user-specific queue
     */
    private <T> void broadcastToUserQueue(Integer userId, String eventType, T payload, String description) {
        String userDestination = getUserQueue(userId);
        RealtimeMessage<T> realtimeMessage = createRealtimeMessage(eventType, payload, description);
        messagingTemplate.convertAndSend(userDestination, realtimeMessage);
    }

    /**
     * Helper method to broadcast to all user queues in a conversation
     */
    private void broadcastToUserQueues(Integer conversationId, String eventType, Object data) {
        List<ConversationParticipant> participants = participantRepository
                .findByConversation_ConversationIdAndIsActiveTrue(conversationId);
        
        for (ConversationParticipant participant : participants) {
            Integer userId = participant.getUser().getUserId();
            broadcastToUserQueue(userId, eventType, data, "User-specific notification");
        }
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
        
        String typingKey = String.format(TYPING_USERS_KEY, conversationId);
        redisTemplate.opsForSet().remove(typingKey, userId.toString());
        
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
        String typingKey = String.format(TYPING_USERS_KEY, conversationId);
        redisTemplate.delete(typingKey);
        
        log.debug("Cleaned up realtime tracking for conversation {}", conversationId);
    }

    private Conversation getConversationEntity(Integer conversationId) {
        return conversationRepository.findById(conversationId)
        .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));
    }

    /**
     * Completely cleans up user presence across all conversations
     */
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