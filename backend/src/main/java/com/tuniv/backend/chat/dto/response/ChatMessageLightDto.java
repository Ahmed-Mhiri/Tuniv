// ChatMessageLightDto.java
package com.tuniv.backend.chat.dto;

import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageLightDto {
    private Integer id;
    private String body;
    private Integer authorId;
    private String authorUsername;
    private Integer conversationId;
    private Instant sentAt;
    private boolean isEdited;
    private String messageType;
    private boolean isPinned;
    private Map<String, Long> reactionCounts;
    private Integer totalReactions;
}