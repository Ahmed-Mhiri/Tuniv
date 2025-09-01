package com.tuniv.backend.notification.mapper;

import com.tuniv.backend.notification.dto.NotificationDto;
import com.tuniv.backend.notification.model.Notification;

public class NotificationMapper {

    public static NotificationDto toDto(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationDto.builder()
                .notificationId(notification.getNotificationId())
                .actorUsername(notification.getActor() != null ? notification.getActor().getUsername() : "System")
                .message(notification.getMessage())
                .link(notification.getLink())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .type(notification.getType())
                .build();
    }
}

