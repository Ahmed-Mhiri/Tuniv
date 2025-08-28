package com.tuniv.backend.chat.dto;

import java.util.List;

import com.tuniv.backend.qa.dto.AttachmentDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageDto {
    private String content;
    private String senderUsername;
    private String sentAt;
    
    // --- FIX: Use a list of Attachment DTOs ---
    private List<AttachmentDto> attachments;
}