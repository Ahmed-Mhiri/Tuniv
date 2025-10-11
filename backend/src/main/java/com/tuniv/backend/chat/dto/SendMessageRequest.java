package com.tuniv.backend.chat.dto;

import com.tuniv.backend.chat.model.MessageType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {
    private String body;
    private Integer replyToMessageId;
    private MessageType messageType; // Change from String to MessageType
    private String clientMessageId; // For client-side tracking
}