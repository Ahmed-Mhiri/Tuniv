package com.tuniv.backend.chat.service;

import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.chat.dto.ChatMessageDto;
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
        // 1. Look up the Conversation and User entities from the database
        User sender = userRepository.findByUsername(senderUsername)
            .orElseThrow(() -> new ResourceNotFoundException("Sender not found with username: " + senderUsername));
            
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));
        
        // 2. Create and save the message entity to the database
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        
        message.setContent(chatMessageDto.getContent());
        message.setFileUrl(chatMessageDto.getFileUrl());
        // --- THIS IS THE FIX ---
        // Changed from chatMessage to chatMessageDto
        message.setFileName(chatMessageDto.getFileName());
        message.setFileType(chatMessageDto.getFileType());
        
        messageRepository.save(message);

        // 3. Broadcast the message to all subscribers of the conversation's topic
        String destination = "/topic/conversation/" + conversationId;
        messagingTemplate.convertAndSend(destination, chatMessageDto);
    }
}