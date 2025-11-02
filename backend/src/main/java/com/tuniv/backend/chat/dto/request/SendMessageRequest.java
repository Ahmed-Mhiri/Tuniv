package com.tuniv.backend.chat.dto.request;

import com.tuniv.backend.chat.model.MessageType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    
    @NotBlank(message = "Message body cannot be empty")
    @Size(max = 5000, message = "Message body cannot exceed 5000 characters")
    private String body;
    
    private Integer replyToMessageId;
    
    @NotNull(message = "Message type is required")
    private MessageType messageType;
    
    @Size(max = 100, message = "Client message ID cannot exceed 100 characters")
    private String clientMessageId;
}