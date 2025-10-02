package com.tuniv.backend.chat.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.dto.ConversationSummaryDto;
import com.tuniv.backend.chat.mapper.ChatMapper;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.model.MessageReaction;
import com.tuniv.backend.chat.model.Reaction;
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.chat.repository.ConversationRepository;
import com.tuniv.backend.chat.repository.MessageRepository;
import com.tuniv.backend.chat.repository.ReactionRepository;
import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.event.NewMessageEvent;
import com.tuniv.backend.qa.service.AttachmentService;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final ReactionRepository reactionRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final AttachmentService attachmentService;
    private final ApplicationEventPublisher eventPublisher;
    private final ChatMapper chatMapper;

    @Transactional
    public Message sendMessage(
            Integer conversationId,
            ChatMessageDto chatMessageDto,
            String senderUsername,
            List<MultipartFile> files
    ) {
        Message savedMessage = saveMessageAndAttachments(conversationId, chatMessageDto, senderUsername, files);

        // ✅ UPDATE: Update conversation's denormalized fields
        Conversation conversation = savedMessage.getConversation();
        conversation.updateLastMessage(savedMessage);
        conversationRepository.save(conversation);

        ChatMessageDto dtoToSend = chatMapper.toChatMessageDto(savedMessage, Collections.emptyList(), senderUsername, chatMessageDto.getClientTempId());

        String destination = "/topic/conversation/" + conversationId;
        messagingTemplate.convertAndSend(destination, dtoToSend);

        Conversation fullConversation = conversationRepository.findByIdWithParticipantsAndUsers(conversationId)
                .orElseThrow(() -> new IllegalStateException("Conversation not found after sending message"));
        User author = savedMessage.getAuthor();
        List<User> recipients = fullConversation.getParticipants().stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> !user.equals(author))
                .collect(Collectors.toList());
        eventPublisher.publishEvent(new NewMessageEvent(this, savedMessage, recipients));

        return savedMessage;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessagesByConversation(Integer conversationId) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!conversationRepository.existsById(conversationId)) {
            throw new ResourceNotFoundException("Conversation not found with id: " + conversationId);
        }

        List<Message> messages = messageRepository.findByConversation_ConversationIdOrderBySentAtAsc(conversationId);
        if (messages.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> messageIds = messages.stream().map(Message::getId).toList();
        List<Reaction> allReactions = reactionRepository.findByPost_IdIn(messageIds);

        Map<Integer, List<Reaction>> reactionsByMessageId = allReactions.stream()
                .collect(Collectors.groupingBy(r -> r.getPost().getId()));

        return messages.stream()
                .map(message -> chatMapper.toChatMessageDto(
                        message,
                        reactionsByMessageId.getOrDefault(message.getId(), Collections.emptyList()),
                        currentUser.getUsername()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChatMessageDto getSingleMessageById(Integer messageId) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));

        List<Reaction> reactions = reactionRepository.findByPost_Id(messageId);
        return chatMapper.toChatMessageDto(message, reactions, currentUser.getUsername());
    }

    @Transactional
    public void deleteMessage(Integer messageId, String currentUsername) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));

        if (!message.getAuthor().getUsername().equals(currentUsername)) {
            throw new SecurityException("User is not authorized to delete this message.");
        }

        message.setDeleted(true);
        message.setBody("");
        messageRepository.save(message);

        ChatMessageDto deletedMessageDto = chatMapper.toChatMessageDto(message, Collections.emptyList(), currentUsername);
        String destination = "/topic/conversation/" + message.getConversation().getConversationId();
        messagingTemplate.convertAndSend(destination, deletedMessageDto);
    }

    @Transactional
    public void toggleReaction(Integer messageId, String emoji, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));

        Optional<Reaction> existingReaction = reactionRepository.findByUser_UserIdAndPost_IdAndEmoji(
                user.getUserId(),
                messageId,
                emoji
        );

        if (existingReaction.isPresent()) {
            reactionRepository.delete(existingReaction.get());
        } else {
            MessageReaction newReaction = new MessageReaction(user, message, emoji);
            reactionRepository.save(newReaction);
        }

        List<Reaction> updatedReactions = reactionRepository.findByPost_Id(messageId);

        ChatMessageDto messageDto = chatMapper.toChatMessageDto(message, updatedReactions, username);
        String destination = "/topic/conversation/" + message.getConversation().getConversationId();
        messagingTemplate.convertAndSend(destination, messageDto);
    }

    @Transactional
    public Message saveMessageAndAttachments(
            Integer conversationId,
            ChatMessageDto chatMessageDto,
            String senderUsername,
            List<MultipartFile> files
    ) {
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found: " + senderUsername));
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));

        Message message = new Message();
        message.setConversation(conversation);
        message.setAuthor(sender);
        String content = chatMessageDto.getContent();
        message.setBody(content == null ? "" : content);
        message.setSentAt(Instant.now());
        Message savedMessage = messageRepository.save(message);

        if (files != null && !files.isEmpty()) {
            attachmentService.saveAttachments(
                    files.stream().filter(Objects::nonNull).collect(Collectors.toList()),
                    savedMessage
            );
        }
        return savedMessage;
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> getConversationSummaries(String username) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        // ✅ UPDATED: Use simple query with denormalized fields
        List<Conversation> conversations = conversationRepository.findConversationsByUser(currentUser.getUserId());
        
        return conversations.stream()
                .map(conv -> mapToConversationSummaryDtoSimple(conv, currentUser))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional
    public ConversationSummaryDto findOrCreateConversation(Integer currentUserId, Integer participantId) {
        if (currentUserId.equals(participantId)) {
            throw new IllegalArgumentException("Cannot start a conversation with oneself.");
        }
        return conversationRepository.findDirectConversationBetweenUsers(currentUserId, participantId)
                .map(conversation -> {
                    User currentUser = userRepository.findById(currentUserId)
                            .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
                    return mapToConversationSummaryDtoSimple(conversation, currentUser);
                })
                .orElseGet(() -> {
                    User currentUser = userRepository.findById(currentUserId)
                            .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
                    User otherParticipant = userRepository.findById(participantId)
                            .orElseThrow(() -> new ResourceNotFoundException("Participant user not found"));
                    Conversation newConversation = new Conversation();
                    Conversation savedConversation = conversationRepository.save(newConversation);
                    ConversationParticipant currentUserParticipant = new ConversationParticipant(currentUser, savedConversation);
                    ConversationParticipant otherUserParticipant = new ConversationParticipant(otherParticipant, savedConversation);
                    conversationParticipantRepository.saveAll(List.of(currentUserParticipant, otherUserParticipant));
                    return ConversationSummaryDto.builder()
                            .conversationId(savedConversation.getConversationId())
                            .participantId(otherParticipant.getUserId())
                            .participantName(otherParticipant.getUsername())
                            .participantAvatarUrl(otherParticipant.getProfilePhotoUrl())
                            .lastMessage("No messages yet...")
                            .lastMessageTimestamp(savedConversation.getCreatedAt().toString())
                            .unreadCount(0L)
                            .build();
                });
    }

    @Transactional
    public void markConversationAsRead(Integer conversationId, String username) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        ConversationParticipant participant = conversationParticipantRepository
                .findByUserUserIdAndConversationConversationId(currentUser.getUserId(), conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found in conversation"));

        participant.setLastReadTimestamp(Instant.now());
        conversationParticipantRepository.save(participant);
    }

    /**
     * ✅ UPDATED: Simple version using denormalized fields
     */
    private ConversationSummaryDto mapToConversationSummaryDtoSimple(Conversation conv, User currentUser) {
        // Get other participant
        User otherParticipant = conv.getParticipants().stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> !user.getUserId().equals(currentUser.getUserId()))
                .findFirst()
                .orElse(null);
        if (otherParticipant == null) return null;

        // ✅ USE DENORMALIZED FIELDS - no extra queries needed for last message!
        String lastMessage = conv.getLastMessageBody() != null ? 
            (conv.getLastMessageBody().length() > 100 ? 
             conv.getLastMessageBody().substring(0, 100) + "..." : 
             conv.getLastMessageBody()) : 
            "No messages yet...";
        
        Instant lastMessageTime = conv.getLastMessageSentAt() != null ?
            conv.getLastMessageSentAt() : conv.getCreatedAt();
        
        // Get current user's participant to find last read time
        ConversationParticipant currentParticipant = conv.getParticipants().stream()
                .filter(p -> p.getUser().getUserId().equals(currentUser.getUserId()))
                .findFirst()
                .orElse(null);
        
        long unreadCount = 0;
        if (currentParticipant != null && conv.getLastMessageSentAt() != null) {
            Instant lastRead = currentParticipant.getLastReadTimestamp();
            // If last message is after last read time, calculate unread count
            if (lastRead == null || conv.getLastMessageSentAt().isAfter(lastRead)) {
                unreadCount = calculateUnreadCount(conv.getConversationId(), currentUser.getUserId(), lastRead);
            }
        }

        return ConversationSummaryDto.builder()
                .conversationId(conv.getConversationId())
                .participantId(otherParticipant.getUserId())
                .participantName(otherParticipant.getUsername())
                .participantAvatarUrl(otherParticipant.getProfilePhotoUrl())
                .lastMessage(lastMessage)
                .lastMessageTimestamp(lastMessageTime.toString())
                .unreadCount(unreadCount)
                .build();
    }

    /**
     * ✅ HELPER: Calculate unread count only when needed
     */
    private long calculateUnreadCount(Integer conversationId, Integer userId, Instant lastReadTimestamp) {
        if (lastReadTimestamp == null) {
            // If never read, count all messages from other users
            return messageRepository.countByConversation_ConversationIdAndAuthor_UserIdNotAndSentAtAfter(
                conversationId, userId, Instant.EPOCH
            );
        } else {
            // Count messages after last read time
            return messageRepository.countByConversation_ConversationIdAndAuthor_UserIdNotAndSentAtAfter(
                conversationId, userId, lastReadTimestamp
            );
        }
    }

    /**
     * ✅ NEW: Method to update conversation last message when a message is sent
     * This ensures denormalized fields stay in sync
     */
    @Transactional
    public void updateConversationLastMessage(Message message) {
        if (!message.isDeleted() && message.getConversation() != null) {
            Conversation conversation = message.getConversation();
            conversation.updateLastMessage(message);
            conversationRepository.save(conversation);
        }
    }

    /**
     * ✅ NEW: Get conversation with participants efficiently
     */
    @Transactional(readOnly = true)
    public Optional<Conversation> getConversationWithParticipants(Integer conversationId) {
        return conversationRepository.findByIdWithParticipantsAndUsers(conversationId);
    }

    /**
     * ✅ NEW: Check if user can access conversation
     */
    @Transactional(readOnly = true)
    public boolean canUserAccessConversation(Integer conversationId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        return conversationParticipantRepository
                .findByUserUserIdAndConversationConversationId(user.getUserId(), conversationId)
                .isPresent();
    }

    /**
     * ✅ NEW: Get unread count for a specific conversation
     */
    @Transactional(readOnly = true)
    public long getUnreadCountForConversation(Integer conversationId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        ConversationParticipant participant = conversationParticipantRepository
                .findByUserUserIdAndConversationConversationId(user.getUserId(), conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("User not in conversation"));
        
        Instant sinceTimestamp = participant.getLastReadTimestamp() != null ? 
            participant.getLastReadTimestamp() : 
            participant.getConversation().getCreatedAt();
        
        return messageRepository.countByConversation_ConversationIdAndAuthor_UserIdNotAndSentAtAfter(
            conversationId, user.getUserId(), sinceTimestamp
        );
    }

    /**
     * ✅ NEW: Get total unread count across all conversations
     */
    @Transactional(readOnly = true)
    public long getTotalUnreadCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        List<Conversation> conversations = conversationRepository.findConversationsByUser(user.getUserId());
        
        long totalUnread = 0;
        for (Conversation conv : conversations) {
            ConversationParticipant participant = conv.getParticipants().stream()
                    .filter(p -> p.getUser().getUserId().equals(user.getUserId()))
                    .findFirst()
                    .orElse(null);
            
            if (participant != null) {
                Instant sinceTimestamp = participant.getLastReadTimestamp() != null ? 
                    participant.getLastReadTimestamp() : conv.getCreatedAt();
                
                long unread = messageRepository.countByConversation_ConversationIdAndAuthor_UserIdNotAndSentAtAfter(
                    conv.getConversationId(), user.getUserId(), sinceTimestamp
                );
                totalUnread += unread;
            }
        }
        
        return totalUnread;
    }
}