// SystemMessageDto.java
package com.tuniv.backend.chat.dto.event;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemMessageDto {
    private String type;
    private String message;
    private Instant timestamp = Instant.now(); // Initialize directly
    private Map<String, Object> metadata;
    private Integer conversationId;

    // Remove the explicit constructor - @NoArgsConstructor handles it
}