package com.tuniv.backend.chat.service;

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

    /**
     * Orchestrates sending a message by first saving it to the database
     * and then broadcasting it via WebSockets. Not transactional itself.
     */
    public Message sendMessage(
            Integer conversationId,
            ChatMessageDto chatMessageDto,
            String senderUsername,
            List<MultipartFile> files
    ) {
        Message messageWithAttachments = saveMessageAndAttachments(conversationId, chatMessageDto, senderUsername, files);
        ChatMessageDto dtoToSend = ChatMapper.toChatMessageDto(messageWithAttachments);

        String destination = "/topic/conversation/" + conversationId;
        messagingTemplate.convertAndSend(destination, dtoToSend);
        eventPublisher.publishEvent(new NewMessageEvent(this, messageWithAttachments));

        return messageWithAttachments;
    }

    /**
     * Handles all database interactions for saving a message and its attachments
     * within a single transaction.
     */
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

        // ✅ FIX: Use setAuthor() for consistency with the Post model.
        // The custom setter in the Message entity will also handle setting the 'sender' field.
        message.setAuthor(sender);

        String content = chatMessageDto.getContent();
        message.setBody(content == null ? "" : content);
        message.setSentAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);

        if (files != null && !files.isEmpty()) {
            attachmentService.saveAttachments(
                    files.stream().filter(Objects::nonNull).collect(Collectors.toList()),
                    savedMessage
            );
        }

        // ✅ FIX: The re-fetch is unnecessary. The 'savedMessage' object is managed by
        // the transaction and is ready to be returned.
        return savedMessage;
    }

    /**
     * Fetches the historical messages for a single conversation.
     */
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

    /**
     * Fetches a single message by its ID.
     */
    @Transactional(readOnly = true)
    public ChatMessageDto getSingleMessageById(Integer messageId) {
        return messageRepository.findById(messageId)
                .map(ChatMapper::toChatMessageDto)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));
    }
    
    // ... other methods in the service remain the same ...

    /**
     * Retrieves a summary list of all conversations for a given user,
     * including unread message counts.
     */
    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> getConversationSummaries(String username) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        List<Conversation> conversations = conversationRepository.findConversationsByUserId(currentUser.getUserId());

        return conversations.stream()
                .map(conv -> mapToConversationSummaryDto(conv, currentUser))
                .filter(Objects::nonNull) // Filter out malformed conversations
                .collect(Collectors.toList());
    }

    /**
     * Marks all messages in a conversation as read for the current user
     * by updating their `lastReadTimestamp`.
     */
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

    /**
     * A private helper method to map entity data to the ConversationSummaryDto.
     */
    private ConversationSummaryDto mapToConversationSummaryDto(Conversation conv, User currentUser) {
        User otherParticipant = conv.getParticipants().stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> !user.getUserId().equals(currentUser.getUserId()))
                .findFirst()
                .orElse(null);

        if (otherParticipant == null) {
            return null;
        }

        Optional<Message> lastMessageOpt = messageRepository.findTopByConversationConversationIdOrderBySentAtDesc(conv.getConversationId());

        LocalDateTime lastReadTimestamp = conv.getParticipants().stream()
                .filter(p -> p.getUser().getUserId().equals(currentUser.getUserId()))
                .findFirst()
                .map(ConversationParticipant::getLastReadTimestamp)
                .orElse(conv.getCreatedAt());

        long unreadCount = messageRepository.countByConversationConversationIdAndSenderUserIdNotAndSentAtAfter(
                conv.getConversationId(),
                currentUser.getUserId(),
                lastReadTimestamp
        );

        return ConversationSummaryDto.builder()
                .conversationId(conv.getConversationId())
                .participantName(otherParticipant.getUsername())
                .participantAvatarUrl(otherParticipant.getProfilePhotoUrl())
                // ✅ FIX: Use getBody() here as well.
                .lastMessage(lastMessageOpt.map(Message::getBody).orElse("No messages yet..."))
                .lastMessageTimestamp(lastMessageOpt.map(m -> m.getSentAt().toString()).orElse(conv.getCreatedAt().toString()))
                .unreadCount((int) unreadCount)
                .build();
    }
}