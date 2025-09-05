package com.tuniv.backend.chat.dto;

import java.util.List;

import com.tuniv.backend.qa.dto.AttachmentDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageDto {
    private Integer messageId;
    private Long clientTempId; // ✅ ADD THIS: To accept the temporary ID from the client
    private String content;
    private String senderUsername;
    private String sentAt;
    private List<AttachmentDto> attachments;
    private boolean isDeleted; // ✅ ADD THIS
    private List<ReactionDto> reactions; // ✅ ADD THIS

}