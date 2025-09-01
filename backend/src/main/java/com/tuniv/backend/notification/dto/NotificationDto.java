package com.tuniv.backend.notification.dto;

import java.time.LocalDateTime;

import com.tuniv.backend.notification.model.NotificationType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationDto {
    private Integer notificationId;
    private String actorUsername;
    private String message;
    private String link;
    private boolean isRead;
    private LocalDateTime createdAt;
    private NotificationType type;
}
