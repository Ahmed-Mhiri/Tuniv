package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageDto {
    private String content;
    private String senderUsername;
    private String sentAt;

    // --- NEW FIELDS ---
    private String fileUrl;
    private String fileName;
    private String fileType;
}