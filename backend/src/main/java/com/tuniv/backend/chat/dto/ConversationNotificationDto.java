package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConversationNotificationDto {
    private Integer conversationId;
    private String title;
    private String conversationType;
    private Integer participantCount;

    public ConversationNotificationDto(Integer conversationId, String title, String conversationType, Integer participantCount) {
        this.conversationId = conversationId;
        this.title = title;
        this.conversationType = conversationType;
        this.participantCount = participantCount;
    }
}