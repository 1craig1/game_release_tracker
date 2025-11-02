package com.gamereleasetracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamereleasetracker.dto.WishlistItemDto;
import com.gamereleasetracker.model.Game;
import com.gamereleasetracker.model.Role;
import com.gamereleasetracker.model.RoleType;
import com.gamereleasetracker.model.User;
import com.gamereleasetracker.model.WishlistItem;
import com.gamereleasetracker.model.WishlistItemId;
import com.gamereleasetracker.repository.GameRepository;
import com.gamereleasetracker.repository.RoleRepository;
import com.gamereleasetracker.repository.UserRepository;
import com.gamereleasetracker.repository.WishlistItemRepository;
import com.gamereleasetracker.service.GameUpdateService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;


// @SpringBootTest
@AutoConfigureMockMvc
// @ActiveProfiles("test")
@Transactional
class WishlistItemControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

    @MockitoBean
    private GameUpdateService gameUpdateService;

    private User user1;
    private Game game1, game2, game3;

    @BeforeEach
    public void setUp() {
        // Clear repositories to ensure a clean state for each test
        wishlistItemRepository.deleteAll();
        userRepository.deleteAll();
        gameRepository.deleteAll();

        // Create and save the Role first
        Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(RoleType.ROLE_USER);
                    return roleRepository.save(newRole);
                });
        userRole.setName(RoleType.ROLE_USER);

        // Create User WITHOUT setting the ID manually
        user1 = new User();
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        user1.setPasswordHash("password");
        user1.setEnableNotifications(true);
        user1.setRole(userRole); // Correctly associate the role
        
        // Use saveAndFlush to ensure the ID is generated and assigned immediately
        userRepository.saveAndFlush(user1);

        // Create Games with more realistic data
        game1 = new Game();
        game1.setTitle("Elden Ring");
        game1.setRawgGameSlug("elden-ring");
        game1.setReleaseDate(LocalDate.now().minusYears(1));
        gameRepository.save(game1);

        game2 = new Game();
        game2.setTitle("Starfield");
        game2.setRawgGameSlug("starfield");
        game2.setReleaseDate(LocalDate.now().minusMonths(2));
        gameRepository.save(game2);

        game3 = new Game();
        game3.setTitle("New Unwishlisted Game");
        game3.setRawgGameSlug("new-unwishlisted-game");
        game3.setReleaseDate(LocalDate.now());
        gameRepository.saveAndFlush(game3);
        
        // 1. Create the composite ID from the saved entities' IDs
        WishlistItemId wishlistItemId = new WishlistItemId(user1.getId(), game1.getId());

        // 2. Create the WishlistItem instance
        WishlistItem wishlistItem = new WishlistItem();
        wishlistItem.setId(wishlistItemId);
        wishlistItem.setUser(user1);
        wishlistItem.setGame(game1);
        
        // 3. Save the correctly constructed item
        wishlistItemRepository.save(wishlistItem);
    }

    // ========== GET /api/users/{userId}/wishlist/games Tests ==========

    @Test
    @WithMockUser
    void list_withExistingUser_returnsWishlistGames() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/wishlist/games", user1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(game1.getId().intValue())))
                .andExpect(jsonPath("$[0].title", is("Elden Ring")));
    }

    @Test
    @WithMockUser
    void list_withNonExistentUser_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/users/99999/wishlist/games"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("User not found")));
    }

    // ========== GET /api/users/{userId}/wishlist/games/{gameId}/exists Tests ==========

    @Test
    @WithMockUser
    void exists_whenItemIsInWishlist_returnsTrue() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/wishlist/games/{gameId}/exists", user1.getId(), game1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists", is(true)));
    }

    @Test
    @WithMockUser
    void exists_whenItemIsNotInWishlist_returnsFalse() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/wishlist/games/{gameId}/exists", user1.getId(), game2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists", is(false)));
    }

    // ========== POST /api/users/{userId}/wishlist Tests ==========

    @Test
    @WithMockUser
    void add_withValidData_returnsCreated() throws Exception {
        WishlistItemDto newWishlistItemDto = new WishlistItemDto(user1.getId(), game3.getId());

        mockMvc.perform(post("/api/users/{userId}/wishlist", user1.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newWishlistItemDto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", String.format("/api/users/%d/wishlist/games/%d", user1.getId(), game3.getId())))
                .andExpect(jsonPath("$.userId", is(user1.getId().intValue())))
                .andExpect(jsonPath("$.gameId", is(game3.getId().intValue())));
    }

    @Test
    @WithMockUser
    void add_whenItemAlreadyExists_returnsConflict() throws Exception {
        WishlistItemDto existingWishlistItemDto = new WishlistItemDto(user1.getId(), game1.getId());

        mockMvc.perform(post("/api/users/{userId}/wishlist", user1.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(existingWishlistItemDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("Wishlist item already exists")));
    }

    @Test
    @WithMockUser
    void add_withNonExistentGame_returnsNotFound() throws Exception {
        WishlistItemDto nonExistentGameDto = new WishlistItemDto(user1.getId(), 99999L);

        mockMvc.perform(post("/api/users/{userId}/wishlist", user1.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistentGameDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Game not found")));
    }

    // ========== DELETE /api/users/{userId}/wishlist Tests ==========

    @Test
    @WithMockUser
    void remove_withExistingItem_returnsNoContent() throws Exception {
        WishlistItemDto itemToRemoveDto = new WishlistItemDto(user1.getId(), game1.getId());

        mockMvc.perform(delete("/api/users/{userId}/wishlist", user1.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemToRemoveDto)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void remove_withNonExistentItem_returnsNotFound() throws Exception {
        WishlistItemDto itemToRemoveDto = new WishlistItemDto(user1.getId(), game2.getId()); // game2 is not in wishlist

        mockMvc.perform(delete("/api/users/{userId}/wishlist", user1.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemToRemoveDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Wishlist item not found")));
    }
}
