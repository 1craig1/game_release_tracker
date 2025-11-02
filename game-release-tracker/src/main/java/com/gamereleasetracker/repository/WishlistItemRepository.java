package com.gamereleasetracker.repository;

import com.gamereleasetracker.model.Game;
import com.gamereleasetracker.model.User;
import com.gamereleasetracker.model.WishlistItem;
import com.gamereleasetracker.model.WishlistItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, WishlistItemId> {
    /**
     * Retrieves all Game entities from a specific user's wishlist.
     * This uses a custom JPQL Query to directly select the game
     * object from each WishlistItem where the user ID matches the
     * provided :userId parameter, making the data retrieval more efficient.
     *
     * @param userId The ID of the user.
     * @return A list of games on the user's wishlist.
     */
    @Query("SELECT w.game FROM WishlistItem w WHERE w.user.id = :userId")
    List<Game> findGamesByUserId(@Param("userId") Long userId);

    /**
     * Checks if a specific game already exists on a user's wishlist.
     * @param user The user entity.
     * @param game The game entity.
     * @return true if the item exists, false otherwise.
     */
    boolean existsByUserAndGame(User user, Game game);
    
    /**
     * Finds all wishlist items associated with a specific game ID.
     * @param gameId The ID of the game.
     * @return A list of wishlist items for the specified game.
     */
    List<WishlistItem> findByGameId(Long gameId);

    /**
     * Finds all wishlist items for a given list of game IDs.
     * This is efficient for batch operations.
     * @param gameIds A list of game IDs.
     * @return A list of wishlist items corresponding to the provided game IDs.
     */
    List<WishlistItem> findByGameIdIn(List<Long> gameIds);
}
