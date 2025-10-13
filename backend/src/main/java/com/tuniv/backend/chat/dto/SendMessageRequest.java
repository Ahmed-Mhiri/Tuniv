// SendMessageRequest.java
package com.tuniv.backend.chat.dto;

import com.tuniv.backend.chat.model.MessageType;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    private String body;
    private Integer replyToMessageId;
    private MessageType messageType;
    private String clientMessageId;
}