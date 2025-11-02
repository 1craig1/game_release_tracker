package com.gamereleasetracker.controller;

import com.gamereleasetracker.dto.GameSummaryDto;
import com.gamereleasetracker.dto.WishlistItemDto;
import com.gamereleasetracker.exception.DuplicateResourceException;
import com.gamereleasetracker.exception.NotFoundException;
import com.gamereleasetracker.service.WishlistItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing a user's wishlist.
 *
 * Base path: /api/users/{userId}/wishlist
 *
 * Conventions:
 * - JSON request/response.
 * - POST/DELETE expect a body shaped like WishlistItemDto: { "gameId": <Long> }
 * - Errors:
 *   - 404 Not Found when user/game does not exist.
 *   - 409 Conflict when an item already exists in the wishlist.
 */
@RestController
@RequestMapping("/api/users/{userId}/wishlist")
public class WishlistItemController {

    private final WishlistItemService wishlistItemService;

    public WishlistItemController(WishlistItemService wishlistItemService) {
        this.wishlistItemService = wishlistItemService;
    }

    /**
     * List all games in a user's wishlist.
     *
     * GET /api/users/{userId}/wishlist/games
     *
     * @param userId ID of the user whose wishlist is requested
     * @return 200 OK with a JSON array of GameSummaryDto
     *
     * Example response:
     * [
     *   { "id": 123, "title": "Elden Ring", "coverImageUrl": "...", "releaseDate": "2024-02-25", "status": "RELEASED" }
     * ]
     */
    @GetMapping("/games")
    public List<GameSummaryDto> list(@PathVariable Long userId) {
        return wishlistItemService.getWishlistItemsByUserId(userId);
    }

    /**
     * Check whether a specific game is in a user's wishlist.
     *
     * GET /api/users/{userId}/wishlist/games/{gameId}/exists
     *
     * @param userId user ID
     * @param gameId game ID
     * @return 200 OK with {"exists": true|false}
     */
    @GetMapping("/games/{gameId}/exists")
    public Map<String, Boolean> exists(@PathVariable Long userId, @PathVariable Long gameId) {
        boolean present = wishlistItemService.isGameInWishlist(userId, gameId);
        return Map.of("exists", present);
    }

    /**
     * Add a game to a user's wishlist.
     *
     * POST /api/users/{userId}/wishlist
     * Body: { "gameId": <Long> }
     *
     * @param body WishlistItemDto containing userId and gameId
     * @return 201 Created with the saved WishlistItemDto in the body,
     *         and a Location header like /api/wishlist/{userId}/games/{gameId}
     *
     * Errors:
     * - 404 if user or game cannot be found
     * - 409 if the wishlist item already exists
     */
    @PostMapping
    public ResponseEntity<WishlistItemDto> add(@PathVariable Long userId, @RequestBody WishlistItemDto body) {
        WishlistItemDto saved = wishlistItemService.addWishlistItem(
                new WishlistItemDto(userId, body.gameId()));
        URI location = URI.create(String.format("/api/users/%d/wishlist/games/%d",
                saved.userId(), saved.gameId()));
        return ResponseEntity.created(location).body(saved);
    }

    /**
     * Remove a game from a user's wishlist.
     *
     * DELETE /api/users/{userId}/wishlist
     * Body: { "gameId": <Long> }
     *
     * @param body WishlistItemDto containing userId and gameId
     * @return 204 No Content on success
     *
     * Errors:
     * - 404 if the wishlist item (userId, gameId) does not exist
     */
    @DeleteMapping
    public ResponseEntity<Void> remove(@PathVariable Long userId, @RequestBody WishlistItemDto body) {
        wishlistItemService.removeWishlistItem(new WishlistItemDto(userId, body.gameId()));
        return ResponseEntity.noContent().build();
    }

    // ---------------- Exception → HTTP mapping ----------------

    /** Map domain "not found" errors to HTTP 404 with a simple JSON error payload. */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    /** Map duplicate resource errors to HTTP 409 with a simple JSON error payload. */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DuplicateResourceException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }
}
