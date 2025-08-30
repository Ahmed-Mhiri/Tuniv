package com.tuniv.backend.chat.service;

import java.time.LocalDateTime;
import java.util.List; // <-- IMPORT ADDED
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.mapper.ChatMapper;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.repository.ConversationRepository;
import com.tuniv.backend.chat.repository.MessageRepository;
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
    private final SimpMessageSendingOperations messagingTemplate;
    private final AttachmentService attachmentService;

    /**
     * This is the main public method. It is NOT transactional.
     * Its job is to orchestrate saving and then broadcasting.
     */
    public void sendMessage(
            Integer conversationId,
            ChatMessageDto chatMessageDto,
            String senderUsername,
            List<MultipartFile> files
    ) {
        // Step 1: Call the transactional method to save everything to the database.
        // When this method returns, the transaction is complete and the data is committed.
        Message messageWithAttachments = saveMessageAndAttachments(conversationId, chatMessageDto, senderUsername, files);

        // Step 2: Now that the data is saved, map the complete entity to a DTO.
        ChatMessageDto dtoToSend = ChatMapper.toChatMessageDto(messageWithAttachments);
        
        // Step 3: Broadcast the final, complete DTO over the WebSocket.
        String destination = "/topic/conversation/" + conversationId;
        messagingTemplate.convertAndSend(destination, dtoToSend);
    }

    /**
     * This new, private method handles ALL database interactions in its own transaction.
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
        message.setSender(sender);
        message.setContent(chatMessageDto.getContent());
        message.setSentAt(LocalDateTime.now());
        
        Message savedMessage = messageRepository.save(message);

        if (files != null && !files.isEmpty()) {
            attachmentService.saveAttachments(
                files.stream().filter(Objects::nonNull).collect(Collectors.toList()),
                savedMessage.getMessageId(),
                "MESSAGE"
            );
        }

        // Re-fetch the message to ensure it includes the attachments before returning.
        return messageRepository.findById(savedMessage.getMessageId())
            .orElseThrow(() -> new ResourceNotFoundException("Failed to re-fetch message after saving attachments"));
    }

    /**
     * Fetches the historical messages for a conversation.
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
}