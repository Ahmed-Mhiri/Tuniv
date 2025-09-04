package com.tuniv.backend.chat.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List; // <-- IMPORT ADDED
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.dto.ConversationSummaryDto;
import com.tuniv.backend.chat.mapper.ChatMapper;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.ConversationParticipant;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.repository.ConversationParticipantRepository;
import com.tuniv.backend.chat.repository.ConversationRepository;
import com.tuniv.backend.chat.repository.MessageRepository;
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
        
        // ✅ Use the new mapper to include the clientTempId in the DTO for broadcast.
        // This is the key change that makes the round-trip work.
        ChatMessageDto dtoToSend = ChatMapper.toChatMessageDto(savedMessage, chatMessageDto.getClientTempId());

        String destination = "/topic/conversation/" + conversationId;
        messagingTemplate.convertAndSend(destination, dtoToSend);

        // This logic remains the same.
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
        if (!conversationRepository.existsById(conversationId)) {
            throw new ResourceNotFoundException("Conversation not found with id: " + conversationId);
        }
        return messageRepository.findByConversationConversationIdOrderBySentAtAsc(conversationId)
                .stream()
                .map(ChatMapper::toChatMessageDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChatMessageDto getSingleMessageById(Integer messageId) {
        return messageRepository.findById(messageId)
                .map(ChatMapper::toChatMessageDto)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));
    }

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