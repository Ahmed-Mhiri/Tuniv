// ConversationNotificationDto.java
package com.tuniv.backend.chat.dto.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationNotificationDto {
    private Integer conversationId;
    private String title;
    private String conversationType;
    private Integer participantCount;
}