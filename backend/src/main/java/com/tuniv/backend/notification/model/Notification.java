package com.tuniv.backend.notification.model;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;

import com.tuniv.backend.user.model.User;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

import org.hibernate.annotations.Where;

import com.tuniv.backend.shared.model.Auditable;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_recipient", columnList = "recipient_id, created_at DESC"),
    @Index(name = "idx_notification_read_status", columnList = "is_read, created_at"),
    @Index(name = "idx_notification_type", columnList = "type, created_at DESC"),
    @Index(name = "idx_notification_actor", columnList = "actor_id, created_at"),
    @Index(name = "idx_notification_expiry", columnList = "expires_at"),
    @Index(name = "idx_notification_source", columnList = "source_entity_type, source_entity_id"), // ✅ ADDED
    @Index(name = "idx_notification_priority", columnList = "priority, created_at DESC") // ✅ ADDED

})
@Getter
@Setter
@ToString(exclude = {"recipient", "actor"}) // Avoid lazy loading issues in logs
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "is_dismissed = false") // ✅ ADD: Global filter for non-dismissed notifications
public class Notification extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer notificationId;

    // ========== OPTIMISTIC LOCKING ==========
    @Version
    private Long version;

    // ========== RECIPIENT & ACTOR ==========
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    // ========== NOTIFICATION CONTENT ==========
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @NotBlank
    @Size(max = 255)
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Size(max = 1000)
    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "link", length = 500)
    private String link; // Frontend route, e.g., "/questions/123#answer-456"

    // ========== STATUS & DELIVERY ==========
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "is_delivered", nullable = false)
    private boolean isDelivered = false;

    @Column(name = "is_dismissed", nullable = false)
    private boolean isDismissed = false;

    @Column(name = "priority", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationPriority priority = NotificationPriority.NORMAL;

    // ========== TIMESTAMPS ==========
    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "dismissed_at")
    private Instant dismissedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    // ========== METADATA ==========
    @Column(name = "source_entity_type", length = 50)
    private String sourceEntityType; // e.g., "QUESTION", "ANSWER", "MESSAGE"

    @Column(name = "source_entity_id")
    private Integer sourceEntityId;

    @Column(name = "action_required", nullable = false)
    private boolean actionRequired = false;

    @Column(name = "action_label", length = 50)
    private String actionLabel;

    @Column(name = "action_link", length = 500)
    private String actionLink;

    @Column(name = "image_url", length = 500)
    private String imageUrl;
    // ========== EQUALS & HASHCODE ==========
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return notificationId != null && Objects.equals(notificationId, that.notificationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notificationId);
    }

    

}
