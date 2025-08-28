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
    private final AttachmentService attachmentService; // <-- INJECT

    @Transactional
    public void sendMessage(
            Integer conversationId,
            ChatMessageDto chatMessageDto,
            String senderUsername,
            List<MultipartFile> files // <-- ADD files parameter
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
        
        // Save the message first to get an ID
        Message savedMessage = messageRepository.save(message);

        // --- FIX: Use the AttachmentService to handle files ---
        if (files != null && !files.isEmpty()) {
            attachmentService.saveAttachments(
                files.stream().filter(Objects::nonNull).collect(Collectors.toList()),
                savedMessage.getMessageId(),
                "MESSAGE"
            );
        }

        // Fetch the message again to get it with the newly saved attachments
        Message messageWithAttachments = messageRepository.findById(savedMessage.getMessageId()).get();
        ChatMessageDto dtoToSend = ChatMapper.toChatMessageDto(messageWithAttachments);
        
        String destination = "/topic/conversation/" + conversationId;
        messagingTemplate.convertAndSend(destination, dtoToSend);
    }

    // --- NEW METHOD ---
    public List<ChatMessageDto> getMessagesByConversation(Integer conversationId) {
        if (!conversationRepository.existsById(conversationId)) {
            throw new ResourceNotFoundException("Conversation not found with id: " + conversationId);
        }
        
        return messageRepository.findByConversationConversationIdOrderBySentAtAsc(conversationId)
                .stream()
                .map(ChatMapper::toChatMessageDto) // Use the mapper
                .collect(Collectors.toList());
    }
}