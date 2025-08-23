package com.tuniv.backend.chat.mapper;

import java.time.format.DateTimeFormatter;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.model.Message;

public class ChatMapper {

    public static ChatMessageDto toChatMessageDto(Message message) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setContent(message.getContent());
        dto.setSenderUsername(message.getSender().getUsername());
        dto.setSentAt(message.getSentAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        dto.setFileUrl(message.getFileUrl());
        dto.setFileName(message.getFileName());
        dto.setFileType(message.getFileType());
        return dto;
    }
}