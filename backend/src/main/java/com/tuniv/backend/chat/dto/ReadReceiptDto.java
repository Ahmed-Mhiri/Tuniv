package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class ReadReceiptDto {
    private Integer userId;
    private String username;
    private String profilePhotoUrl;
    private Integer conversationId;
    private Instant lastReadTimestamp;
    private Integer messageId; // Optional: specific message read
}
