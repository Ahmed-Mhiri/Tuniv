// UserActiveStatusDto.java
package com.tuniv.backend.chat.dto.event;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserActiveStatusDto {
    private Integer userId;
    private Integer conversationId;
    private boolean isActive;
    private Instant updatedAt;
}