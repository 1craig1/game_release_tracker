package com.gamereleasetracker.service;

import com.gamereleasetracker.dto.GameSummaryDto;
import com.gamereleasetracker.dto.GenreDto;
import com.gamereleasetracker.dto.PlatformDto;
import com.gamereleasetracker.dto.WishlistItemDto;
import com.gamereleasetracker.exception.DuplicateResourceException;
import com.gamereleasetracker.exception.NotFoundException;
import com.gamereleasetracker.model.Game;
import com.gamereleasetracker.model.Genre;
import com.gamereleasetracker.model.Platform;
import com.gamereleasetracker.model.User;
import com.gamereleasetracker.model.WishlistItem;
import com.gamereleasetracker.model.WishlistItemId;
import com.gamereleasetracker.repository.GameRepository;
import com.gamereleasetracker.repository.UserRepository;
import com.gamereleasetracker.repository.WishlistItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@Service
public class WishlistItemService {
    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final NotificationService notificationService;

    @Autowired
    public WishlistItemService(WishlistItemRepository wishlistItemRepository,
                               UserRepository userRepository,
                               GameRepository gameRepository,
                               NotificationService notificationService) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.notificationService = notificationService;
    }

    /**
     * Adds a new WishlistItem to the user's wishlist.
     * Should only be executable by the user who owns the wishlist.
     *
     * @param wishlistItemDto The WishlistItemDto containing userId and gameId to be added.
     * @return The saved WishlistItem object.
     */
    @Transactional
    public WishlistItemDto addWishlistItem(WishlistItemDto wishlistItemDto) {
        WishlistItemId newId = new WishlistItemId(wishlistItemDto.userId(), wishlistItemDto.gameId());

        if (wishlistItemRepository.existsById(newId)) {
            throw new DuplicateResourceException("Wishlist item already exists for userId: "
                    + wishlistItemDto.userId() + " and gameId: " + wishlistItemDto.gameId());
        }

        User user = userRepository.findById(wishlistItemDto.userId())
                .orElseThrow(() -> new NotFoundException("User not found with id: " + wishlistItemDto.userId()));
        Game game = gameRepository.findById(wishlistItemDto.gameId())
                .orElseThrow(() -> new NotFoundException("Game not found with id: " + wishlistItemDto.gameId()));

        WishlistItem wishlistItem = new WishlistItem();
        wishlistItem.setId(newId);
        wishlistItem.setUser(user);
        wishlistItem.setGame(game);
        wishlistItem.setAddedAt(java.time.Instant.now());

        WishlistItem savedItem = wishlistItemRepository.save(wishlistItem);

        notificationService.notifyWishlistAddition(user, game);

        return convertToWishlistItemDto(savedItem);
    }

    /**
     * Removes a WishlistItem from the user's wishlist.
     * Should only be executable by the user who owns the wishlist item.
     *
     * @param wishlistItemDto The WishlistItemDto containing userId and gameId to identify the item to be removed.
     */
    public void removeWishlistItem(WishlistItemDto wishlistItemDto) {
        WishlistItem wishlistItem = wishlistItemRepository.findById(
                new WishlistItemId(wishlistItemDto.userId(), wishlistItemDto.gameId()))
                .orElseThrow(() -> new NotFoundException("Wishlist item not found for userId: "
                        + wishlistItemDto.userId() + " and gameId: " + wishlistItemDto.gameId()));
        wishlistItemRepository.delete(wishlistItem);
    }

    /**
     * Retrieves all wishlist items for a specific user.
     *
     * @param userId The ID of the user whose wishlist items are to be retrieved.
     * @return A list of Game objects in the user's wishlist.
     */
    public List<GameSummaryDto> getWishlistItemsByUserId(Long userId) {
        // First, check if the user actually exists.
        if (!userRepository.existsById(userId)) {
            // If not, throw an exception that the controller can handle.
            throw new NotFoundException("User not found with id: " + userId);
        }
        return wishlistItemRepository.findGamesByUserId(userId).stream()
            .map(this::convertToGameSummaryDto)
            .collect(Collectors.toList());
    }

    /**
     * Checks if a specific game is in the user's wishlist.
     *
     * @param userId The ID of the user.
     * @param gameId The ID of the game.
     * @return true if the game is in the wishlist, false otherwise.
     */
    public boolean isGameInWishlist(Long userId, Long gameId) {
        User user = new User();
        Game game = new Game();
        user.setId(userId);
        game.setId(gameId);
        return wishlistItemRepository.existsByUserAndGame(
                user, game);
    }

    public GameSummaryDto convertToGameSummaryDto(Game game) {
        Set<GenreDto> genres = game.getGenres().stream()
                .map(this::toGenreDto)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<PlatformDto> platforms = game.getPlatforms().stream()
                .map(this::toPlatformDto)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new GameSummaryDto(
                game.getId(),
                game.getTitle(),
                game.getDescription(),
                game.getCoverImageUrl(),
                game.getReleaseDate(),
                game.getStatus(),
                genres,
                platforms,
                game.isMature()
        );
    }

    private GenreDto toGenreDto(Genre genre) {
        return new GenreDto(
                genre.getId(),
                genre.getName()
        );
    }

    private PlatformDto toPlatformDto(Platform platform) {
        return new PlatformDto(
                platform.getId(),
                platform.getName()
        );
    }

    public WishlistItemDto convertToWishlistItemDto(WishlistItem item) {
        return new WishlistItemDto(
            item.getId().userId(),
            item.getId().gameId()
        );
    }
}
