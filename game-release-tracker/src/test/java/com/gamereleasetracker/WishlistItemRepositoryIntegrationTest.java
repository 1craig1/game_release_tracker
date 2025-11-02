package com.gamereleasetracker;

import com.gamereleasetracker.model.*;
import com.gamereleasetracker.repository.*;
import com.gamereleasetracker.service.GameUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// @SpringBootTest
// @ActiveProfiles("test") // Ensures this test runs with the 'test' configuration
@Transactional
class WishlistItemRepositoryIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private GameUpdateService gameUpdateService;

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role userRole;

    @BeforeEach
    public void setUp() {
        userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(RoleType.ROLE_USER);
                    return roleRepository.save(newRole);
                });
    }

    /**
     * Tests if a new WishlistItem can be saved and then successfully retrieved
     * from the database using its composite ID.
     */
    @Test
    @Transactional
    void testSaveAndFindWishlistItem() {
        // --- Setup ---
        User user = new User();
        user.setUsername("wishlister");
        user.setEmail("wishlister@example.com");
        user.setPasswordHash("pw");
        user.setRole(userRole);
        User savedUser = userRepository.save(user);

        Game game = new Game();
        game.setTitle("Hollow Knight: Silksong");
        game.setRawgGameSlug("hollow-knight-silksong");
        game.setReleaseDate(LocalDate.now());
        Game savedGame = gameRepository.save(game);

        WishlistItemId wishlistItemId = new WishlistItemId(savedUser.getId(), savedGame.getId());
        WishlistItem newWishlistItem = new WishlistItem();
        newWishlistItem.setId(wishlistItemId);
        newWishlistItem.setUser(savedUser);
        newWishlistItem.setGame(savedGame);

        // --- Action ---
        WishlistItem savedItem = wishlistItemRepository.save(newWishlistItem);

        // --- Assertion ---
        assertThat(savedItem).isNotNull();
        WishlistItem foundItem = wishlistItemRepository.findById(wishlistItemId).orElseThrow();
        assertThat(foundItem.getUser().getUsername()).isEqualTo("wishlister");
        assertThat(foundItem.getGame().getTitle()).isEqualTo("Hollow Knight: Silksong");
    }

    /**
     * Tests the findGamesByUserId custom query to ensure it retrieves all games
     * on a specific user's wishlist, and only their games.
     */
    @Test
    @Transactional
    void testFindGamesByUserId() {
        // --- Setup ---
        User userA = new User();
        userA.setUsername("userA");
        userA.setEmail("usera@example.com");
        userA.setPasswordHash("pw_a");
        userA.setRole(userRole);
        userRepository.save(userA);

        User userB = new User();
        userB.setUsername("userB");
        userB.setEmail("userb@example.com");
        userB.setPasswordHash("pw_b");
        userB.setRole(userRole);
        userRepository.save(userB);

        Game game1 = new Game();
        game1.setTitle("Game 1");
        game1.setRawgGameSlug("game1");
        game1.setReleaseDate(LocalDate.now());
        gameRepository.save(game1);

        Game game2 = new Game();
        game2.setTitle("Game 2");
        game2.setRawgGameSlug("game2");
        game2.setReleaseDate(LocalDate.now());
        gameRepository.save(game2);

        WishlistItem item1 = new WishlistItem();
        item1.setId(new WishlistItemId(userA.getId(), game1.getId()));
        item1.setUser(userA);
        item1.setGame(game1);

        WishlistItem item2 = new WishlistItem();
        item2.setId(new WishlistItemId(userA.getId(), game2.getId()));
        item2.setUser(userA);
        item2.setGame(game2);
        wishlistItemRepository.saveAll(Arrays.asList(item1, item2));

        // --- Action ---
        List<Game> userAWishlist = wishlistItemRepository.findGamesByUserId(userA.getId());

        // --- Assertion ---
        assertThat(userAWishlist).hasSize(2);
        assertThat(userAWishlist).extracting(Game::getTitle).containsExactlyInAnyOrder("Game 1", "Game 2");
    }

    /**
     * Tests the existsByUserAndGame custom query to verify it correctly checks
     * for the existence of a specific game on a user's wishlist.
     */
    @Test
    @Transactional
    void testCheckIfWishlistItemExistsByUserAndGame() {
        // --- Setup ---
        User user = new User();
        user.setUsername("checker");
        user.setEmail("checker@example.com");
        user.setPasswordHash("pw");
        user.setRole(userRole);
        userRepository.save(user);

        Game gameOnList = new Game();
        gameOnList.setTitle("Game On List");
        gameOnList.setRawgGameSlug("gameOnList");
        gameOnList.setReleaseDate(LocalDate.now());
        gameRepository.save(gameOnList);

        Game gameNotOnList = new Game();
        gameNotOnList.setTitle("Game Not On List");
        gameNotOnList.setRawgGameSlug("gameNotOnList");
        gameNotOnList.setReleaseDate(LocalDate.now());
        gameRepository.save(gameNotOnList);

        WishlistItem item = new WishlistItem();
        item.setId(new WishlistItemId(user.getId(), gameOnList.getId()));
        item.setUser(user);
        item.setGame(gameOnList);
        wishlistItemRepository.save(item);

        // --- Action ---
        boolean isOnList = wishlistItemRepository.existsByUserAndGame(user, gameOnList);
        boolean isNotOnList = wishlistItemRepository.existsByUserAndGame(user, gameNotOnList);

        // --- Assertion ---
        assertThat(isOnList).isTrue();
        assertThat(isNotOnList).isFalse();
    }
}