package com.tuniv.backend.chat.service;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.model.Message;
// Other imports...
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {
    
    private final MessageRepository messageRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    
    @Transactional
    public void sendMessage(Integer conversationId, ChatMessageDto chatMessageDto, String senderUsername) {
        // Here you would look up the Conversation and User entities from the database
        // For simplicity, we'll assume they are valid for now.
        
        // 1. Create and save the message entity to the database
        Message message = new Message();
        // message.setConversation(...)
        // message.setSender(...)
        message.setContent(chatMessageDto.getContent());
        messageRepository.save(message);

        // 2. Broadcast the message to all subscribers of the conversation's topic
        String destination = "/topic/conversation/" + conversationId;
        messagingTemplate.convertAndSend(destination, chatMessageDto);
    }
}