package com.gamereleasetracker;

import com.gamereleasetracker.model.Role;
import com.gamereleasetracker.model.RoleType;
import com.gamereleasetracker.model.User;
import com.gamereleasetracker.repository.RoleRepository;
import com.gamereleasetracker.repository.UserRepository;
import com.gamereleasetracker.service.GameUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

// @SpringBootTest
// @ActiveProfiles("test") // Ensures this test runs with the 'test' configuration
@Transactional
class UserRepositoryIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private GameUpdateService gameUpdateService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role userRole;

    @BeforeEach
    public void setUp() {
        /* Try to find "ROLE_USER". If success, userRole gets that role.
         Else, the lambda code runs to create the role, and 'userRole' gets the newly
         created one. This guarantees 'userRole' will not be null. */
        userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(RoleType.ROLE_USER);
                    return roleRepository.save(newRole);
                });
    }

    /**
     * Tests that users and their roles are saved correctly in the database
     * and can be found via user ID
     */
    @Test
    @Transactional
    void testSaveAndFindUser() {
        // --- Setup ---
        User newUser = new User();
        newUser.setUsername("testuser");
        newUser.setEmail("test@example.com");
        newUser.setPasswordHash("pw");
        newUser.setRole(userRole);

        // --- Action ---
        User savedUser = userRepository.save(newUser);

        // --- Assertion ---
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isGreaterThan(0);

        User foundUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(foundUser.getUsername()).isEqualTo("testuser");
        assertThat(foundUser.getRole().getName()).isEqualTo(RoleType.ROLE_USER);
    }

    /**
     * Tests the findByUsername method to ensure it correctly finds a user with the matching username,
     * and that it correctly cannot find a non-existent user
     */
    @Test
    @Transactional
    void testFindUserByUsername() {
        // --- Setup ---
        User user = new User();
        user.setUsername("find_me");
        user.setEmail("find_me@example.com");
        user.setPasswordHash("pw");
        user.setRole(userRole);
        userRepository.save(user);

        // --- Action ---
        User foundUser = userRepository.findByUsername("find_me").orElseThrow();
        boolean userNotFound = userRepository.findByUsername("non_existent_user").isPresent();

        // --- Assertion ---
        assertThat(foundUser.getEmail()).isEqualTo("find_me@example.com");
        assertThat(userNotFound).isFalse();
    }

    /**
     * Tests the findByEmail method to ensure it correctly finds a user with the matching email,
     * and that it correctly cannot find a non-existent email.
     */
    @Test
    @Transactional
    void testFindUserByEmail() {
        // --- Setup ---
        User user = new User();
        user.setUsername("by_email");
        user.setEmail("find_by_email@example.com");
        user.setPasswordHash("pw");
        user.setRole(userRole);
        userRepository.save(user);

        // --- Action ---
        User foundUser = userRepository.findByEmail("find_by_email@example.com").orElseThrow();
        boolean userNotFound = userRepository.findByEmail("non_existent@example.com").isPresent();

        // --- Assertion ---
        assertThat(foundUser.getUsername()).isEqualTo("by_email");
        assertThat(userNotFound).isFalse();
    }

    /**
     * Tests the existsByUsername method to ensure it correctly determines whether a username exists
     */
    @Test
    @Transactional
    void testCheckIfUsernameExists() {
        // --- Setup ---
        User user = new User();
        user.setUsername("existing_user");
        user.setEmail("exists@example.com");
        user.setPasswordHash("pw");
        user.setRole(userRole);
        userRepository.save(user);

        // --- Action ---
        boolean exists = userRepository.existsByUsername("existing_user");
        boolean notExists = userRepository.existsByUsername("non_existent_user");

        // --- Assertion ---
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    /**
     * Tests the existsByEmail method to ensure it correctly determines whether an email exists
     */
    @Test
    @Transactional
    void testCheckIfEmailExists() {
        // --- Setup ---
        User user = new User();
        user.setUsername("email_exists_user");
        user.setEmail("email_exists@example.com");
        user.setPasswordHash("pw");
        user.setRole(userRole);
        userRepository.save(user);

        // --- Action ---
        boolean exists = userRepository.existsByEmail("email_exists@example.com");
        boolean notExists = userRepository.existsByEmail("non_existent@example.com");

        // --- Assertion ---
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}