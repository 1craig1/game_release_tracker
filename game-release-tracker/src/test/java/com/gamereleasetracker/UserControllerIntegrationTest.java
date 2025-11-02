package com.gamereleasetracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamereleasetracker.dto.LoginRequestDto;
import com.gamereleasetracker.dto.UserCreateRequestDto;
import com.gamereleasetracker.dto.UserDto;
import com.gamereleasetracker.dto.PasswordUpdateDto;
import com.gamereleasetracker.model.Role;
import com.gamereleasetracker.model.RoleType;
import com.gamereleasetracker.model.User;
import com.gamereleasetracker.repository.PersistentLoginRepository;
import com.gamereleasetracker.repository.RoleRepository;
import com.gamereleasetracker.repository.UserRepository;
import com.gamereleasetracker.service.GameUpdateService;
import com.gamereleasetracker.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

/**
 * Integration tests for the AuthController to validate authentication and user registration functionalities.
 * These tests simulate real-world use cases of the authentication service in an isolated environment using
 * MockMvc and a transactional test database.
 * Annotations:
 * - {@code @SpringBootTest}: Loads the complete application context for testing.
 * - {@code @AutoConfigureMockMvc}: Configures MockMvc which is used to send HTTP requests to the API.
 * - {@code @Transactional}: Ensures that test data is rolled back after each test to maintain isolation.
 * - {@code @ActiveProfiles("test")}: Uses the 'test' profile configuration during testing.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) 
@Transactional
@ActiveProfiles("test") // Ensures this test runs with the 'test' configuration
class UserControllerIntegrationTest {

    @MockitoBean
    private GameUpdateService gameUpdateService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PersistentLoginRepository persistentLoginRepository;

    private Role userRole;

    private Role adminRole;

    @BeforeEach
    void setUp() {
        // Ensures the "ROLE_USER" role exists, consistent with UserRepositoryIntegrationTest
        userRole = roleRepository.findByName(RoleType.ROLE_USER).orElseGet(() -> {
            Role newUserRole = new Role();
            newUserRole.setName(RoleType.ROLE_USER);
            return roleRepository.save(newUserRole);
        });

        adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN).orElseGet(() -> {
            Role newAdminRole = new Role();
            newAdminRole.setName(RoleType.ROLE_ADMIN);
            return roleRepository.save(newAdminRole);
        });
    }

    @Test
    @WithMockUser(username = "testuser1")
    void testGetCurrentUser_Authenticated() throws Exception {
        // Create a user in the database that matches the mock user
        createTestUser("testuser1", "test1@example.com", "password123");

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser1")))
                .andExpect(jsonPath("$.email", is("test1@example.com")));
    }

    @Test
    void testGetCurrentUser_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser2")
    void testUpdateUser_Authenticated() throws Exception {
        // Create a user in the database
        User user = createTestUser("testuser2", "test2@example.com", "password123");
        UserDto updatedUserDto = new UserDto(user.getId(), "updatedUser", "updated@example.com", true, userRole.getName());
        String updatedUserJson = objectMapper.writeValueAsString(updatedUserDto);
        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedUserJson)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("updatedUser")))
                .andExpect(jsonPath("$.email", is("updated@example.com")));
    }

    @Test
    @WithMockUser(username = "testuser3")
    void testUpdateUser_Forbidden() throws Exception {
        // Create a user in the database
        User user = createTestUser("testuser3", "test3@example.com", "password123");
        UserDto updatedUserDto = new UserDto(1234L, "updatedUser", "updated@example.com", true, userRole.getName());
        String updatedUserJson = objectMapper.writeValueAsString(updatedUserDto);
        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedUserJson)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateUser_Unauthorised() throws Exception {
        // Create a user in the database
        UserDto updatedUserDto = new UserDto(1234L, "updatedUser", "updated@example.com", true, RoleType.ROLE_USER);
        String updatedUserJson = objectMapper.writeValueAsString(updatedUserDto);
        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedUserJson)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser4")
    void testUpdatePassword_Authenticated() throws Exception {
        // Create a user in the database
        createTestUser("testuser4", "test4@example.com", "password123");
        PasswordUpdateDto passwordUpdateDto = new PasswordUpdateDto("password123", "newPassword456", "newPassword456");
        String passwordUpdateJson = objectMapper.writeValueAsString(passwordUpdateDto);

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordUpdateJson)
                        .with(csrf()))
                .andExpect(status().isNoContent());
        // Verify the password was updated
        User updatedUser = userRepository.findByUsername("testuser4").orElseThrow();
        assertThat(passwordEncoder.matches("newPassword456", updatedUser.getPasswordHash())).isTrue();
    }

    @Test
    void testUpdatePassword_unAuthenticated() throws Exception {
        PasswordUpdateDto passwordUpdateDto = new PasswordUpdateDto("password123", "newPassword456", "newPassword456");
        String passwordUpdateJson = objectMapper.writeValueAsString(passwordUpdateDto);

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordUpdateJson)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser5")
    void testUpdatePassword_DeleteCurrentUser() throws Exception {
        // Create a user in the database
        createTestUser("testuser5", "test5@example.com", "password123");

        mockMvc.perform(delete("/api/users/me")
                        .with(csrf()))
                .andExpect(status().isNoContent());
        // Verify the user is deleted
        assertThat(userRepository.findByUsername("testuser5")).isEmpty();
    }

    @Test
    void testDeleteCurrentUser_Unauthenticated() throws Exception {
        mockMvc.perform(delete("/api/users/me")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    void testViewUserById_Admin() throws Exception {
        User user = createTestUser("viewbyiduser", "viewbyiduser@example.com", "password123");
        mockMvc.perform(get("/api/users/admin/id/" + user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("viewbyiduser")))
                .andExpect(jsonPath("$.email", is("viewbyiduser@example.com")));
    }

    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    void testViewUserByUserName_Admin() throws Exception {
        User user = createTestUser("viewbynameuser", "viewbynameuser@example.com", "password123");
        mockMvc.perform(get("/api/users/admin/username/" + user.getUsername()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("viewbynameuser")))
                .andExpect(jsonPath("$.email", is("viewbynameuser@example.com")));
    }

    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    void testViewUsers_Admin() throws Exception {
        createTestUser("users1", "users1@example.com", "password123");
        createTestUser("users2", "users2@example.com", "password123");
        createTestUser("users3", "users3@example.com", "password123");

        mockMvc.perform(get("/api/users/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.[*].username", hasItems("users1", "users2", "users3")));
    }

    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    void testUpdateUserRole_Admin() throws Exception {
        User admin = createTestUser("adminuser", "adminuser@example.com", "password123");
        admin.setRole(adminRole);
        userRepository.save(admin);
        User userToUpdate = createTestUser("roleupdateuser", "roleupdateuser@example.com", "password123");
        mockMvc.perform(put("/api/users/admin/" + userToUpdate.getId() + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"ROLE_ADMIN\"")
                        .with(csrf()))
                .andExpect(status().isOk());
        User updatedUser = userRepository.findById(userToUpdate.getId()).orElseThrow();
        assertThat(updatedUser.getRole().getName()).isEqualTo(RoleType.ROLE_ADMIN);
    }

    /**
     * Creates a test user with the specified username, email, and password.
     * The password is hashed before being saved in the database, and the user is
     * assigned a default role.
     *
     * @param username the username of the test user
     * @param email the email of the test user
     * @param password the raw password of the test user, which will be hashed
     */
    private User createTestUser(String username, String email, String password) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(userRole);
        user.setEnableNotifications(true);

        return userRepository.save(user);
    }
}