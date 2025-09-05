package com.tuniv.backend.chat.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List; // <-- IMPORT ADDED
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
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.chat.repository.ConversationRepository;
import com.tuniv.backend.chat.repository.MessageRepository;
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
    private final SimpMessageSendingOperations messagingTemplate;
    private final AttachmentService attachmentService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Message sendMessage(
            Integer conversationId,
            ChatMessageDto chatMessageDto,
            String senderUsername,
            List<MultipartFile> files
    ) {
        Message savedMessage = saveMessageAndAttachments(conversationId, chatMessageDto, senderUsername, files);

        // ✅ Use the updated mapper that includes the clientTempId for the round-trip
        ChatMessageDto dtoToSend = ChatMapper.toChatMessageDto(savedMessage, senderUsername, chatMessageDto.getClientTempId());

        String destination = "/topic/conversation/" + conversationId;
        messagingTemplate.convertAndSend(destination, dtoToSend);

        // Notification logic remains the same
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
    public List<ChatMessageDto> getMessagesByConversation(Integer conversationId) {
        // ✅ Get the current user's context to correctly map reactions
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!conversationRepository.existsById(conversationId)) {
            throw new ResourceNotFoundException("Conversation not found with id: " + conversationId);
        }
        return messageRepository.findByConversationConversationIdOrderBySentAtAsc(conversationId)
                .stream()
                // ✅ Use the consolidated mapper with the username to include reaction data
                .map(message -> ChatMapper.toChatMessageDto(message, currentUser.getUsername()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChatMessageDto getSingleMessageById(Integer messageId) {
        // ✅ Get the current user's context to correctly map reactions
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return messageRepository.findById(messageId)
                // ✅ Use the consolidated mapper with the username
                .map(message -> ChatMapper.toChatMessageDto(message, currentUser.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));
    }

    @Transactional
    public void deleteMessage(Integer messageId, String currentUsername) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));

        if (!message.getAuthor().getUsername().equals(currentUsername)) {
            throw new SecurityException("User is not authorized to delete this message.");
        }

        message.setDeleted(true);
        message.setBody(""); // Clear content
        messageRepository.save(message);

        // ✅ Broadcast the deletion event using the mapper with username context
        ChatMessageDto deletedMessageDto = ChatMapper.toChatMessageDto(message, currentUsername);
        String destination = "/topic/conversation/" + message.getConversation().getConversationId();
        messagingTemplate.convertAndSend(destination, deletedMessageDto);
    }

    @Transactional
    public void toggleReaction(Integer messageId, String emoji, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        Optional<MessageReaction> existingReaction = message.getReactions().stream()
                .filter(r -> r.getUser().equals(user) && r.getEmoji().equals(emoji))
                .findFirst();

        if (existingReaction.isPresent()) {
            message.getReactions().remove(existingReaction.get());
        } else {
            MessageReaction newReaction = new MessageReaction();
            newReaction.setId(new MessageReaction.MessageReactionId(messageId, user.getUserId(), emoji));
            newReaction.setMessage(message);
            newReaction.setUser(user);
            newReaction.setEmoji(emoji);
            message.getReactions().add(newReaction);
        }

        Message updatedMessage = messageRepository.save(message);

        // Broadcast the full updated message. This logic is correct.
        ChatMessageDto messageDto = ChatMapper.toChatMessageDto(updatedMessage, username);
        String destination = "/topic/conversation/" + message.getConversation().getConversationId();
        messagingTemplate.convertAndSend(destination, messageDto);
    }

    // --- Methods below this line did not require changes ---

    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> getConversationSummaries(String username) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return conversationRepository.findConversationSummariesForUser(currentUser.getUserId());
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
                    return mapToConversationSummaryDto(conversation, currentUser);
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
        participant.setLastReadTimestamp(LocalDateTime.now());
        conversationParticipantRepository.save(participant);
    }

    private ConversationSummaryDto mapToConversationSummaryDto(Conversation conv, User currentUser) {
        User otherParticipant = conv.getParticipants().stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> !user.getUserId().equals(currentUser.getUserId()))
                .findFirst()
                .orElse(null);
        if (otherParticipant == null) return null;
        Optional<Message> lastMessageOpt = messageRepository.findTopByConversationConversationIdOrderBySentAtDesc(conv.getConversationId());
        LocalDateTime lastReadTimestamp = conv.getParticipants().stream()
                .filter(p -> p.getUser().getUserId().equals(currentUser.getUserId()))
                .findFirst()
                .map(ConversationParticipant::getLastReadTimestamp)
                .orElse(conv.getCreatedAt());
        long unreadCount = messageRepository.countByConversationConversationIdAndAuthorUserIdNotAndSentAtAfter(
                conv.getConversationId(),
                currentUser.getUserId(),
                lastReadTimestamp
        );
        return ConversationSummaryDto.builder()
                .conversationId(conv.getConversationId())
                .participantId(otherParticipant.getUserId())
                .participantName(otherParticipant.getUsername())
                .participantAvatarUrl(otherParticipant.getProfilePhotoUrl())
                .lastMessage(lastMessageOpt.map(Message::getBody).orElse("No messages yet..."))
                .lastMessageTimestamp(lastMessageOpt.map(m -> m.getSentAt().toString()).orElse(conv.getCreatedAt().toString()))
                .unreadCount(unreadCount)
                .build();
    }
}