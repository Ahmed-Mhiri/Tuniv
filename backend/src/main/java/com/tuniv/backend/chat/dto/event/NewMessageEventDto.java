package com.tuniv.backend.chat.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewMessageEventDto {
    private Integer messageId;
    private Integer conversationId;
    private Instant sentAt;
    private Integer authorId; 
    private String bodyPreview;
}