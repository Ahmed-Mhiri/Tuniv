package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor; // ✅ ADDED
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor // ✅ ADDED: This creates the default no-argument constructor
public class ReadReceiptDto {
    private Integer userId;
    private String username;
    private String profilePhotoUrl;
    private Integer conversationId;
    private Instant lastReadTimestamp;
    private Integer lastReadMessageId;
    private Instant readAt;

    // Convenience constructor
    public ReadReceiptDto(Integer userId, String username, Integer conversationId,
                         Integer lastReadMessageId, Instant lastReadTimestamp) {
        this.userId = userId;
        this.username = username;
        this.conversationId = conversationId;
        this.lastReadMessageId = lastReadMessageId;
        this.lastReadTimestamp = lastReadTimestamp;
        this.readAt = Instant.now();
    }
}