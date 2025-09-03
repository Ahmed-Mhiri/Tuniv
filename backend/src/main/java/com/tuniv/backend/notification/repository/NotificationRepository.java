package com.tuniv.backend.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.notification.model.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    // Spring Data JPA will automatically create a method implementation for this
    // It will find all notifications for a specific recipient, ordered by creation date descending
    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Integer userId);
    // ✅ ADD THIS METHOD
    List<Notification> findAllByRecipientUserIdAndIsReadIsFalse(Integer userId);

     @Transactional
    void deleteByNotificationIdAndRecipient_UserId(Integer notificationId, Integer userId);

    /**
     * Deletes all notifications for a specific user.
     *
     * BEFORE (Incorrect): void deleteAllByUserUserId(Integer userId);
     * AFTER  (Correct):  Now matches the 'recipient' field in the Notification entity.
     */
    @Transactional
    void deleteAllByRecipient_UserId(Integer userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.userId = :userId AND n.isRead = false")
    void markAllAsReadForUser(Integer userId);

    
}
