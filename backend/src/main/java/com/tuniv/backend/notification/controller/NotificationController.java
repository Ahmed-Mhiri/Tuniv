package com.tuniv.backend.notification.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.notification.dto.NotificationDto;
import com.tuniv.backend.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * GET /api/v1/notifications
     * Fetches all notifications for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<NotificationDto>> getMyNotifications(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<NotificationDto> notifications = notificationService.getNotificationsForUser(currentUser);
        return ResponseEntity.ok(notifications);
    }

    /**
     * POST /api/v1/notifications/{notificationId}/read
     * Marks a specific notification as read.
     */
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(
            @PathVariable Integer notificationId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        notificationService.markAsRead(notificationId, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/notifications/mark-all-read
     * Marks all of the user's notifications as read.
     */
    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllNotificationsAsRead(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        notificationService.markAllAsRead(currentUser);
        return ResponseEntity.noContent().build();
    }
}
