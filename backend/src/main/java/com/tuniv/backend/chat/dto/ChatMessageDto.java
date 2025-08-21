package com.tuniv.backend.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ChatMessageDto {
    private String content;
    private String senderUsername;
    private String sentAt;
}