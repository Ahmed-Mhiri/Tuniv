package com.tuniv.backend.chat.mapper;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.stream.Collectors;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.qa.mapper.QAMapper;

public class ChatMapper {

    public static ChatMessageDto toChatMessageDto(Message message) {
        if (message == null) return null;

        ChatMessageDto dto = new ChatMessageDto();
        dto.setContent(message.getContent());
        dto.setSenderUsername(message.getSender().getUsername());
        dto.setSentAt(message.getSentAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // --- FIX: Map the attachments collection ---
        dto.setAttachments(
            message.getAttachments() != null ?
                message.getAttachments().stream()
                    .map(QAMapper::toAttachmentDto) // Reuse the existing attachment mapper
                    .collect(Collectors.toList()) :
                Collections.emptyList()
        );
        
        return dto;
    }
}