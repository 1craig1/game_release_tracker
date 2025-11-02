package com.gamereleasetracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.gamereleasetracker.model.Game;
import com.gamereleasetracker.model.GameStatus;
import com.gamereleasetracker.model.Genre;
import com.gamereleasetracker.model.Notification;
import com.gamereleasetracker.model.Role;
import com.gamereleasetracker.model.RoleType;
import com.gamereleasetracker.model.User;
import com.gamereleasetracker.model.Platform;
import com.gamereleasetracker.repository.GameRepository;
import com.gamereleasetracker.repository.GenreRepository;
import com.gamereleasetracker.repository.NotificationRepository;
import com.gamereleasetracker.repository.PlatformRepository;
import com.gamereleasetracker.repository.RoleRepository;
import com.gamereleasetracker.repository.UserRepository;
import com.gamereleasetracker.service.GameUpdateService;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @SpringBootTest
// @ActiveProfiles("test")

@AutoConfigureMockMvc
@Transactional
class NotificationControllerIntegrationTest extends BaseIntegrationTest {

    // Mock external services if they are not relevant to the controller's web layer logic
    @MockitoBean
    private GameUpdateService gameUpdateService;

    @Autowired
    private MockMvc mockMvc;

    // Repositories for test data setup and verification
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private PlatformRepository platformRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Test entities
    private User testUser;
    private Notification unreadNotification;
    private Notification readNotification;

    @BeforeEach
    public void setUp() {
        // Clean up database before each test
        notificationRepository.deleteAll();
        gameRepository.deleteAll();
        userRepository.deleteAll();
        genreRepository.deleteAll();
        platformRepository.deleteAll();

        // --- Create common entities for tests ---
        Role userRole = roleRepository.findByName(RoleType.ROLE_USER).orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName(RoleType.ROLE_USER);
            return roleRepository.save(newRole);
        });

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("password"));
        testUser.setRole(userRole);
        userRepository.save(testUser);

        // Create a test game with genres and platforms
        Genre rpgGenre = new Genre();
        rpgGenre.setName("RPG");
        genreRepository.save(rpgGenre);

        Genre actionGenre = new Genre();
        actionGenre.setName("Action");
        genreRepository.save(actionGenre);

        Platform pcPlatform = new Platform();
        pcPlatform.setName("PC");
        platformRepository.save(pcPlatform);

        Platform ps5Platform = new Platform();
        ps5Platform.setName("PS5");
        platformRepository.save(ps5Platform);

        Game testGame = new Game();
        testGame.setTitle("Elden Ring");
        testGame.setRawgGameSlug("elden-ring");
        testGame.setStatus(GameStatus.RELEASED);
        testGame.setReleaseDate(LocalDate.of(2022, 2, 25));
        testGame.setDeveloper("FromSoftware");
        testGame.setGenres(new HashSet<>(Set.of(rpgGenre, actionGenre)));
        testGame.setPlatforms(new HashSet<>(Set.of(pcPlatform, ps5Platform)));
        testGame = gameRepository.save(testGame);

        // Create one unread and one read notification for the testUser
        unreadNotification = new Notification();
        unreadNotification.setUser(testUser);
        unreadNotification.setGame(testGame);
        unreadNotification.setMessage("A new DLC is available!");
        unreadNotification.setRead(false);
        notificationRepository.save(unreadNotification);

        readNotification = new Notification();
        readNotification.setUser(testUser);
        readNotification.setGame(testGame);
        readNotification.setMessage("Your pre-order is confirmed.");
        readNotification.setRead(true);
        notificationRepository.save(readNotification);
    }

    // --- Nested Test Classes for Each Endpoint ---

    @Nested
    @DisplayName("GET /api/notifications")
    class GetNotificationsTests {
        @Test
        @WithMockUser(username = "testuser")
        void getNotifications_ShouldReturnAllNotificationsForUser() throws Exception {
            mockMvc.perform(get("/api/notifications").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$.[*].message", containsInAnyOrder(
                            "A new DLC is available!",
                            "Your pre-order is confirmed."
                    )));
        }

        @Test
        void getNotifications_Unauthenticated_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/notifications").with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/notifications/unread")
    class GetUnreadNotificationsTests {
        @Test
        @WithMockUser(username = "testuser")
        void getUnreadNotifications_ShouldReturnOnlyUnread() throws Exception {
            mockMvc.perform(get("/api/notifications/unread").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(unreadNotification.getId().intValue())))
                    .andExpect(jsonPath("$[0].message", is(unreadNotification.getMessage())));
        }

        @Test
        void getUnreadNotifications_Unauthenticated_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/notifications/unread").with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/notifications/unread/count")
    class GetUnreadCountTests {
        @Test
        @WithMockUser(username = "testuser")
        void getUnreadCount_ShouldReturnCorrectCount() throws Exception {
            mockMvc.perform(get("/api/notifications/unread/count").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count", is(1)));
        }

        @Test
        void getUnreadCount_Unauthenticated_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/notifications/unread/count").with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/notifications/{id}/read")
    class MarkAsReadTests {
        @Test
        @WithMockUser(username = "testuser")
        void markAsRead_ShouldUpdateNotificationStatus() throws Exception {
            mockMvc.perform(put("/api/notifications/" + unreadNotification.getId() + "/read").with(csrf()))
                    .andExpect(status().isNoContent());

            Notification updatedNotification = notificationRepository.findById(unreadNotification.getId()).get();
            assertTrue(updatedNotification.isRead());
        }

        @Test
        void markAsRead_Unauthenticated_ShouldReturn401() throws Exception {
            mockMvc.perform(put("/api/notifications/" + unreadNotification.getId() + "/read").with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "testuser")
        void markAsRead_NotFound_ShouldReturn404() throws Exception {
            long nonExistentId = 9999L;
            mockMvc.perform(put("/api/notifications/" + nonExistentId + "/read").with(csrf()))
                    .andExpect(status().isNotFound()); // Assuming service layer throws an exception mapped to 404
        }
    }

    @Nested
    @DisplayName("PUT /api/notifications/{id}/unread")
    class MarkAsUnreadTests {
        @Test
        @WithMockUser(username = "testuser")
        void markAsUnread_ShouldUpdateNotificationStatus() throws Exception {
            mockMvc.perform(put("/api/notifications/" + readNotification.getId() + "/unread").with(csrf()))
                    .andExpect(status().isNoContent());

            Notification updatedNotification = notificationRepository.findById(readNotification.getId()).get();
            assertFalse(updatedNotification.isRead());
        }

        @Test
        void markAsUnread_Unauthenticated_ShouldReturn401() throws Exception {
            mockMvc.perform(put("/api/notifications/" + readNotification.getId() + "/unread").with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }
    
    @Nested
    @DisplayName("PUT /api/notifications/read-all")
    class MarkAllAsReadTests {
        @Test
        @WithMockUser(username = "testuser")
        void markAllAsRead_ShouldUpdateAllUserNotifications() throws Exception {
            mockMvc.perform(put("/api/notifications/read-all").with(csrf()))
                    .andExpect(status().isNoContent());

            List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
            assertTrue(notifications.stream().allMatch(Notification::isRead));
        }

        @Test
        void markAllAsRead_Unauthenticated_ShouldReturn401() throws Exception {
            mockMvc.perform(put("/api/notifications/read-all").with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /api/notifications/{id}")
    class DeleteNotificationTests {
        @Test
        @WithMockUser(username = "testuser")
        void deleteNotification_ShouldRemoveNotification() throws Exception {
            mockMvc.perform(delete("/api/notifications/" + unreadNotification.getId()).with(csrf()))
                    .andExpect(status().isNoContent());

            assertFalse(notificationRepository.findById(unreadNotification.getId()).isPresent());
        }

        @Test
        void deleteNotification_Unauthenticated_ShouldReturn401() throws Exception {
            mockMvc.perform(delete("/api/notifications/" + unreadNotification.getId()).with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Security: Access Control")
    class SecurityTests {
        @Test
        @WithMockUser(username = "anotheruser")
        void accessAnotherUsersNotification_ShouldBeForbidden() throws Exception {
            // Create the 'anotheruser'
            Role userRole = roleRepository.findByName(RoleType.ROLE_USER).get();
            User anotherUser = new User();
            anotherUser.setUsername("anotheruser");
            anotherUser.setEmail("anotheruser@example.com");
            anotherUser.setPasswordHash(passwordEncoder.encode("password"));
            anotherUser.setRole(userRole);
            userRepository.save(anotherUser);

            // 'anotheruser' has no notifications
            mockMvc.perform(get("/api/notifications").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            // Attempt to modify a notification belonging to 'testuser' should fail.
            // A 404 Not Found is a common, secure response to prevent leaking information
            // about the existence of a resource. A 403 Forbidden is also acceptable.
            
            // Attempt to mark as read
            mockMvc.perform(put("/api/notifications/" + unreadNotification.getId() + "/read").with(csrf()))
                    .andExpect(status().isNotFound());

            // Attempt to delete
            mockMvc.perform(delete("/api/notifications/" + unreadNotification.getId()).with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }
}