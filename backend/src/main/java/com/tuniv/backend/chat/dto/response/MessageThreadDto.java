// MessageThreadDto.java
package com.tuniv.backend.chat.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageThreadDto {
    private ChatMessageDto parentMessage;
    private List<ChatMessageDto> replies;
    private Integer replyCount;
    private boolean hasMoreReplies;
    private Integer threadStarterId; // Add this field
    private Integer conversationId;  // Add this field
}