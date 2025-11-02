package com.gamereleasetracker.controller;

import com.gamereleasetracker.dto.NotificationDto;
import com.gamereleasetracker.service.NotificationService;
import com.gamereleasetracker.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing user notifications.
 *
 * Base path: /api/notifications
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    /**
     * Get all notifications for the current user.
     *
     * GET /api/notifications
     *
     * @param principal The authenticated user
     * @return 200 OK with a list of notification DTOs
     */
    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = userService.getUserByUsername(principal.getName()).id();
        List<NotificationDto> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications for the current user.
     *
     * GET /api/notifications/unread
     *
     * @param principal The authenticated user
     * @return 200 OK with a list of unread notification DTOs
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = userService.getUserByUsername(principal.getName()).id();
        List<NotificationDto> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get the count of unread notifications for the current user.
     *
     * GET /api/notifications/unread/count
     *
     * @param principal The authenticated user
     * @return 200 OK with {"count": <number>}
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = userService.getUserByUsername(principal.getName()).id();
        long count = notificationService.getUnreadNotificationCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Mark a notification as read.
     *
     * PUT /api/notifications/{id}/read
     *
     * @param id The notification ID
     * @param principal The authenticated user
     * @return 204 No Content on success
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = userService.getUserByUsername(principal.getName()).id();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Mark a notification as unread.
     *
     * PUT /api/notifications/{id}/unread
     *
     * @param id The notification ID
     * @param principal The authenticated user
     * @return 204 No Content on success
     */
    @PutMapping("/{id}/unread")
    public ResponseEntity<Void> markAsNotRead(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = userService.getUserByUsername(principal.getName()).id();
        notificationService.markAsNotRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Mark all notifications as read for the current user.
     *
     * PUT /api/notifications/read-all
     *
     * @param principal The authenticated user
     * @return 204 No Content on success
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = userService.getUserByUsername(principal.getName()).id();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete a notification.
     *
     * DELETE /api/notifications/{id}
     *
     * @param id The notification ID
     * @param principal The authenticated user
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = userService.getUserByUsername(principal.getName()).id();
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.noContent().build();
    }
}