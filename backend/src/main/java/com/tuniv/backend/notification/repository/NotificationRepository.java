package com.tuniv.backend.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.notification.model.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    // Spring Data JPA will automatically create a method implementation for this
    // It will find all notifications for a specific recipient, ordered by creation date descending
    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Integer userId);
    // ✅ ADD THIS METHOD
    List<Notification> findAllByRecipientUserIdAndIsReadIsFalse(Integer userId);
    
}
