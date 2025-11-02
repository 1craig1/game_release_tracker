package com.gamereleasetracker;

import com.gamereleasetracker.model.Game;
import com.gamereleasetracker.model.GameStatus;
import com.gamereleasetracker.model.Notification;
import com.gamereleasetracker.model.User;
import com.gamereleasetracker.model.WishlistItem;
import com.gamereleasetracker.model.WishlistItemId;
import com.gamereleasetracker.service.NotificationService;
import com.gamereleasetracker.exception.NotFoundException;
import com.gamereleasetracker.repository.GameRepository;
import com.gamereleasetracker.repository.NotificationRepository;
import com.gamereleasetracker.repository.UserRepository;
import com.gamereleasetracker.repository.WishlistItemRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;


@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private WishlistItemRepository wishlistItemRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    // --- HELPER METHODS FOR TEST DATA SETUP ---

    private Game createTestGame(Long id, String title, String developer) {
        Game game = new Game();
        game.setId(id);
        game.setTitle(title);
        game.setDeveloper(developer);
        game.setReleaseDate(LocalDate.now().minusDays(1));
        game.setRawgGameSlug(title.toLowerCase().replace(" ", "-"));
        game.setStatus(GameStatus.RELEASED);
        return game;
    }

    private User createTestUser(Long id, String username, boolean notificationsEnabled) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("hashed_password");
        user.setEnableNotifications(notificationsEnabled);
        return user;
    }

    private WishlistItem createTestWishlistItem(User user, Game game) {
        // 1. Create the composite ID
        WishlistItemId wishlistItemId = new WishlistItemId(user.getId(), game.getId());

        // 2. Create the WishlistItem and set its properties
        WishlistItem item = new WishlistItem();
        item.setId(wishlistItemId);
        item.setUser(user);
        item.setGame(game);
        return item;
    }

    @Test
    void notifyUsersOfGameReleases_shouldCreateNotificationsForUsersWithEnabledNotifications() {
        // Arrange
        Game game1 = createTestGame(1L, "Elden Ring", "FromSoftware");
        User user1 = createTestUser(101L, "user1", true);  // Notifications enabled
        User user2 = createTestUser(102L, "user2", false); // Notifications disabled

        WishlistItem item1 = createTestWishlistItem(user1, game1);
        WishlistItem item2 = createTestWishlistItem(user2, game1);

        when(gameRepository.findAllById(List.of(game1.getId()))).thenReturn(List.of(game1));
        when(wishlistItemRepository.findByGameIdIn(List.of(game1.getId()))).thenReturn(List.of(item1, item2));

        // Act
        notificationService.notifyUsersOfGameReleases(List.of(game1.getId()));

        // Assert
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());

        List<Notification> savedNotifications = captor.getValue();
        assertEquals(1, savedNotifications.size());
        assertEquals(user1.getId(), savedNotifications.get(0).getUser().getId());
        assertEquals("'Elden Ring' is now released!", savedNotifications.get(0).getMessage());
    }

    @Test
    void notifyUsersOfGameReleases_whenNoWishlistItems_shouldDoNothing() {
        Game game1 = createTestGame(1L, "Cyberpunk 2077", "CD PROJEKT RED");
        when(gameRepository.findAllById(List.of(game1.getId()))).thenReturn(List.of(game1));
        when(wishlistItemRepository.findByGameIdIn(List.of(game1.getId()))).thenReturn(Collections.emptyList());

        notificationService.notifyUsersOfGameReleases(List.of(game1.getId()));

        verify(notificationRepository, never()).saveAll(any());
    }
    
    @Test
    void createNotification_shouldReturnCorrectlyConfiguredNotification() {
        User user = createTestUser(1L, "testuser", true);
        Game game = createTestGame(10L, "Starfield", "Bethesda Game Studios");
        String message = "A new message";

        Notification notification = notificationService.createNotification(user, game, message);

        assertNotNull(notification);
        assertEquals(user, notification.getUser());
        assertEquals(game, notification.getGame());
        assertEquals(message, notification.getMessage());
        assertFalse(notification.isRead());
    }

    @Test
    void notifyUsersOfGameReleases_whenGameIdsIsNull_shouldReturnEarly() {
        notificationService.notifyUsersOfGameReleases(null);
        verifyNoInteractions(gameRepository, wishlistItemRepository, notificationRepository);
    }

    @Test
    void notifyUsersOfGameReleases_whenGameIdsIsEmpty_shouldReturnEarly() {
        notificationService.notifyUsersOfGameReleases(Collections.emptyList());
        verifyNoInteractions(gameRepository, wishlistItemRepository, notificationRepository);
    }

    @Test
    void markAsNotRead_whenUserIsNotOwner_shouldThrowNotFoundException() {
        User owner = createTestUser(10L, "owner", true);
        Notification notification = new Notification();
        notification.setUser(owner);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        assertThrows(NotFoundException.class, () -> notificationService.markAsNotRead(1L, 20L));
    }

    @Test
    void markAsNotRead_whenNotificationNotFound_shouldThrowNotFoundException() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> notificationService.markAsNotRead(99L, 1L));
    }

    @Test
    void markAsRead_whenNotificationNotFound_shouldThrowNotFoundException() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> notificationService.markAsRead(99L, 1L));
    }

    @Test
    void deleteNotification_whenNotificationNotFound_shouldThrowNotFoundException() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> notificationService.deleteNotification(99L, 1L));
    }
}