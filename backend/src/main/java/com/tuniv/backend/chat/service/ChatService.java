package com.tuniv.backend.chat.service;

import java.time.LocalDateTime;
import java.util.List; // <-- IMPORT ADDED
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.mapper.ChatMapper;
import com.tuniv.backend.chat.model.Conversation;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.repository.ConversationRepository;
import com.tuniv.backend.chat.repository.MessageRepository;
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
    
    @Transactional
    public void sendMessage(Integer conversationId, ChatMessageDto chatMessageDto, String senderUsername) {
        User sender = userRepository.findByUsername(senderUsername)
            .orElseThrow(() -> new ResourceNotFoundException("Sender not found with username: " + senderUsername));
            
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));
        
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(chatMessageDto.getContent());
        message.setFileUrl(chatMessageDto.getFileUrl());
        message.setFileName(chatMessageDto.getFileName());
        message.setFileType(chatMessageDto.getFileType());
        message.setSentAt(LocalDateTime.now());
        
        messageRepository.save(message);

        // Map the saved entity to a DTO before broadcasting
        ChatMessageDto dtoToSend = ChatMapper.toChatMessageDto(message);
        
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