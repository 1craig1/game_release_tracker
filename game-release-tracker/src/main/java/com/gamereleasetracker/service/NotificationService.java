
package com.gamereleasetracker.service;

import com.gamereleasetracker.dto.NotificationDto;
import com.gamereleasetracker.exception.NotFoundException;
import com.gamereleasetracker.model.Game;
import com.gamereleasetracker.model.Notification;
import com.gamereleasetracker.model.User;
import com.gamereleasetracker.model.WishlistItem;
import com.gamereleasetracker.repository.GameRepository;
import com.gamereleasetracker.repository.NotificationRepository;
import com.gamereleasetracker.repository.UserRepository;
import com.gamereleasetracker.repository.WishlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final WishlistItemRepository wishlistItemRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               GameRepository gameRepository,
                               WishlistItemRepository wishlistItemRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.wishlistItemRepository = wishlistItemRepository;
    }

    /**
     * Notifies users who have the specified games in their wishlist about the game releases.
     *
     * @param gameIds The IDs of the games that were released
     */
    @Transactional
    public void notifyUsersOfGameReleases(List<Long> gameIds) {
        if (gameIds == null || gameIds.isEmpty()) {
            return;
        }

        // Fetch all relevant games in one query and map them by ID for easy lookup
        Map<Long, Game> releasedGames = gameRepository.findAllById(gameIds).stream()
                .collect(Collectors.toMap(Game::getId, Function.identity()));

        // Fetch all wishlist items for the released games in one query
        List<WishlistItem> wishlistItems = wishlistItemRepository.findByGameIdIn(gameIds);

        List<Notification> notificationsToSave = new ArrayList<>();
        for (WishlistItem item : wishlistItems) {
            Game game = releasedGames.get(item.getGame().getId());
            User user = item.getUser();
            // Only notify if the game exists and the user has notifications enabled
            if (game != null && user.isEnableNotifications()) {
                String message = String.format("'%s' is now released!", game.getTitle());
                // Create notification
                Notification notification = createNotification(user, game, message);
                // Add to batch list
                notificationsToSave.add(notification);
            }
        }

        if (!notificationsToSave.isEmpty()) {
            // Save all notifications in a single batch operation
            notificationRepository.saveAll(notificationsToSave);
        }
    }

    /**
     * Creates a notification for a specific user about a game.
     *
     * @param user The user to notify
     * @param game The game the notification is about
     * @param message The notification message
     * @return The created notification
     */
    public Notification createNotification(User user, Game game, String message) {

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setGame(game);
        notification.setMessage(message);
        notification.setRead(false);

        return notification;
    }

    /**
     * Sends a notification when a user adds a game to their wishlist.
     *
     * @param user The user who added the game
     * @param game The game that was added
     */
    @Transactional
    public void notifyWishlistAddition(User user, Game game) {
        if (user == null || game == null || !user.isEnableNotifications()) {
            return;
        }

        String message = String.format("'%s' was added to your wishlist. We'll keep you posted!", game.getTitle());
        Notification notification = createNotification(user, game, message);
        notificationRepository.save(notification);
    }

    /**
     * Gets all notifications for a user.
     *
     * @param userId The ID of the user
     * @return A list of notification DTOs
     */
    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets unread notifications for a user.
     *
     * @param userId The ID of the user
     * @return A list of unread notification DTOs
     */
    @Transactional(readOnly = true)
    public List<NotificationDto> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets the count of unread notifications for a user.
     *
     * @param userId The ID of the user
     * @return The count of unread notifications
     */
    @Transactional(readOnly = true)
    public long getUnreadNotificationCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Marks a notification as read.
     *
     * @param notificationId The ID of the notification
     * @param userId The ID of the user (for authorization)
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new NotFoundException("Notification not found");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Marks a notification as not read.
     *
     * @param notificationId The ID of the notification
     * @param userId The ID of the user (for authorization)
     */
    @Transactional
    public void markAsNotRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new NotFoundException("Notification not found");
        }

        notification.setRead(false);
        notificationRepository.save(notification);
    }

    /**
     * Marks all notifications as read for a user.
     *
     * @param userId The ID of the user
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);

        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
        }

        notificationRepository.saveAll(unreadNotifications);
    }

    /**
     * Deletes a notification.
     *
     * @param notificationId The ID of the notification
     * @param userId The ID of the user (for authorization)
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new NotFoundException("Notification not found");
        }

        notificationRepository.delete(notification);
    }

    /**
     * Converts a Notification entity to a NotificationDto.
     */
    private NotificationDto convertToDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getUser().getId(),
                notification.getGame().getId(),
                notification.getGame().getTitle(),
                notification.getGame().getCoverImageUrl(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
