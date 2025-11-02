package com.gamereleasetracker.repository;

import com.gamereleasetracker.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Finds all notifications for a specific user, sort by creation date in descending order.
     * @param userId The ID of the user
     * @return A list of notifications
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Finds all unread notifications for a specific user, sort by creation date in descending order.
     * @param userId The ID of the use
     * @return A list of unread notifications
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * Counts unread notifications for a specific user (for display purposes).
     * @param userId The ID of the user
     * @return The count of unread notifications
     */
    long countByUserIdAndIsReadFalse(Long userId);
}