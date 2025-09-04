package com.tuniv.backend.chat.mapper;

import java.util.Collections;
import java.util.stream.Collectors;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.qa.mapper.QAMapper;

public class ChatMapper {

    /**
     * Overloaded method to carry the clientTempId through to the final DTO.
     * This is the method that will be used before broadcasting over WebSocket.
     */
    public static ChatMessageDto toChatMessageDto(Message message, Long clientTempId) {
        ChatMessageDto dto = toChatMessageDto(message); // Reuse the existing logic
        if (dto != null) {
            dto.setClientTempId(clientTempId); // ✅ SET THE ID for the round trip
        }
        return dto;
    }
    
    /**
     * Original method for general-purpose mapping (e.g., for REST history).
     */
    public static ChatMessageDto toChatMessageDto(Message message) {
        if (message == null) {
            return null;
        }

        ChatMessageDto dto = new ChatMessageDto();
        dto.setMessageId(message.getId());
        dto.setContent(message.getBody());

        if (message.getAuthor() != null) {
            dto.setSenderUsername(message.getAuthor().getUsername());
        } else {
            dto.setSenderUsername("Unknown User");
        }

        if (message.getSentAt() != null) {
            dto.setSentAt(message.getSentAt().toString());
        } else if (message.getCreatedAt() != null) {
            dto.setSentAt(message.getCreatedAt().toString());
        }

        dto.setAttachments(
            message.getAttachments() != null ?
                message.getAttachments().stream()
                    .map(QAMapper::toAttachmentDto) // Assuming you have a QAMapper for attachments
                    .collect(Collectors.toList()) :
                Collections.emptyList()
        );

        return dto;
    }
}