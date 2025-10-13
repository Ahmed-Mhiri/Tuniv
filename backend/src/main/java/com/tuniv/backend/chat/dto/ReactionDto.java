// ReactionDto.java
package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReactionDto {
    private Integer id;
    private Integer messageId;
    private Integer userId;
    private String username;
    private String profilePhotoUrl;
    private String emoji;
    private Integer skinTone;
    private String customText;
    private Instant createdAt;
    private boolean isRemoved;
    private Instant removedAt;
}