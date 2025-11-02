package com.gamereleasetracker;

import com.gamereleasetracker.dto.GameSummaryDto;
import com.gamereleasetracker.dto.WishlistItemDto;
import com.gamereleasetracker.exception.NotFoundException;
import com.gamereleasetracker.model.*;
import com.gamereleasetracker.repository.GameRepository;
import com.gamereleasetracker.repository.UserRepository;
import com.gamereleasetracker.repository.WishlistItemRepository;
import com.gamereleasetracker.service.NotificationService;
import com.gamereleasetracker.service.WishlistItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class WishlistItemServiceUnitTest {

    @Mock
    private WishlistItemRepository wishlistItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private WishlistItemService wishlistItemService;

    private User user;
    private Game game;
    private WishlistItem wishlistItem;
    private WishlistItemDto wishlistItemDto;
    private WishlistItemId wishlistItemId;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        game = new Game();
        game.setId(101L);
        game.setTitle("Stellar Blade");

        wishlistItemId = new WishlistItemId(user.getId(), game.getId());
        wishlistItemDto = new WishlistItemDto(user.getId(), game.getId());

        wishlistItem = new WishlistItem();
        wishlistItem.setId(wishlistItemId);
        wishlistItem.setUser(user);
        wishlistItem.setGame(game);
    }

    @Test
    void testAddWishlistItemSuccess() {
        // --- Setup ---
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
        when(wishlistItemRepository.save(any(WishlistItem.class))).thenReturn(wishlistItem);

        // --- Action ---
        WishlistItemDto result = wishlistItemService.addWishlistItem(wishlistItemDto);

        // --- Assertion ---
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.gameId()).isEqualTo(game.getId());

        // We use an ArgumentCaptor here because the WishlistItem is created inside the service method.
        // We don't have a reference to it, so we need to capture it to check its properties.

        // 1. Create an ArgumentCaptor to capture the WishlistItem object.
        ArgumentCaptor<WishlistItem> wishlistItemCaptor = ArgumentCaptor.forClass(WishlistItem.class);

        // 2. Use the captor inside a verify() call to "capture" the argument that was passed to the save method.
        verify(wishlistItemRepository).save(wishlistItemCaptor.capture());

        // 3. Retrieve the captured object using .getValue().
        WishlistItem capturedItem = wishlistItemCaptor.getValue();

        // Now, perform assertions on the captured object to ensure it was created correctly.
        assertThat(capturedItem.getUser()).isEqualTo(user);
        assertThat(capturedItem.getGame()).isEqualTo(game);
        assertThat(capturedItem.getAddedAt()).isNotNull();

        verify(notificationService).notifyWishlistAddition(user, game);
    }

    @Test
    void testAddWishlistItemThrowsNotFoundExceptionForUser() {
        // --- Setup ---
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        // --- Action & Assertion ---
        assertThrows(NotFoundException.class, () -> {
            wishlistItemService.addWishlistItem(wishlistItemDto);
        });

        verify(gameRepository, never()).findById(any());
        verify(wishlistItemRepository, never()).save(any());
        verify(notificationService, never()).notifyWishlistAddition(any(), any());
    }

    @Test
    void testAddWishlistItemThrowsNotFoundExceptionForGame() {
        // --- Setup ---
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(gameRepository.findById(game.getId())).thenReturn(Optional.empty());

        // --- Action & Assertion ---
        assertThrows(NotFoundException.class, () -> {
            wishlistItemService.addWishlistItem(wishlistItemDto);
        });

        verify(wishlistItemRepository, never()).save(any());
        verify(notificationService, never()).notifyWishlistAddition(any(), any());
    }

    @Test
    void testRemoveWishlistItemSuccess() {
        // --- Setup ---
        when(wishlistItemRepository.findById(wishlistItemId)).thenReturn(Optional.of(wishlistItem));
        doNothing().when(wishlistItemRepository).delete(wishlistItem);

        // --- Action ---
        wishlistItemService.removeWishlistItem(wishlistItemDto);

        // --- Assertion ---
        verify(wishlistItemRepository, times(1)).findById(wishlistItemId);
        verify(wishlistItemRepository, times(1)).delete(wishlistItem);
    }

    @Test
    void testRemoveWishlistItemThrowsNotFoundException() {
        // --- Setup ---
        when(wishlistItemRepository.findById(wishlistItemId)).thenReturn(Optional.empty());

        // --- Action & Assertion ---
        assertThrows(NotFoundException.class, () -> {
            wishlistItemService.removeWishlistItem(wishlistItemDto);
        });

        verify(wishlistItemRepository, never()).delete(any());
    }

    @Test
    void testGetWishlistItemsByUserId() {
        when(userRepository.existsById(user.getId())).thenReturn(true);
        // --- Setup ---
        Game anotherGame = new Game();
        anotherGame.setId(102L);
        anotherGame.setTitle("Dragon's Dogma 2");
        when(wishlistItemRepository.findGamesByUserId(user.getId())).thenReturn(List.of(game, anotherGame));

        // --- Action ---
        List<GameSummaryDto> result = wishlistItemService.getWishlistItemsByUserId(user.getId());

        // --- Assertion ---
        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("Stellar Blade");
        assertThat(result.get(1).title()).isEqualTo("Dragon's Dogma 2");
    }

    @Test
    void testGetWishlistItemsByUserIdReturnsEmptyList() {
        when(userRepository.existsById(user.getId())).thenReturn(true);
        // --- Setup ---
        when(wishlistItemRepository.findGamesByUserId(user.getId())).thenReturn(Collections.emptyList());

        // --- Action ---
        List<GameSummaryDto> result = wishlistItemService.getWishlistItemsByUserId(user.getId());

        // --- Assertion ---
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void testIsGameInWishlistReturnsTrue() {
        // --- Setup ---
        when(wishlistItemRepository.existsByUserAndGame(any(User.class), any(Game.class))).thenReturn(true);

        // --- Action ---
        boolean result = wishlistItemService.isGameInWishlist(user.getId(), game.getId());

        // --- Assertion ---
        assertThat(result).isTrue();

        // The service method creates new User and Game objects internally, just setting their IDs.
        // We use captors to verify that these objects were created with the correct IDs.

        // 1. Create captors for both the User and Game objects.
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);

        // 2. Capture the arguments passed to the repository method.
        verify(wishlistItemRepository).existsByUserAndGame(userCaptor.capture(), gameCaptor.capture());

        // 3. Retrieve the captured objects and assert that the correct IDs were used to create them.
        assertThat(userCaptor.getValue().getId()).isEqualTo(user.getId());
        assertThat(gameCaptor.getValue().getId()).isEqualTo(game.getId());
    }

    @Test
    void testIsGameInWishlistReturnsFalse() {
        // --- Setup ---
        when(wishlistItemRepository.existsByUserAndGame(any(User.class), any(Game.class))).thenReturn(false);

        // --- Action ---
        boolean result = wishlistItemService.isGameInWishlist(user.getId(), game.getId());

        // --- Assertion ---
        assertThat(result).isFalse();
    }
}
