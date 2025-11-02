package com.gamereleasetracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamereleasetracker.dto.LoginRequestDto;
import com.gamereleasetracker.dto.UserCreateRequestDto;
import com.gamereleasetracker.model.Role;
import com.gamereleasetracker.model.RoleType;
import com.gamereleasetracker.model.User;
import com.gamereleasetracker.repository.PersistentLoginRepository;
import com.gamereleasetracker.repository.RoleRepository;
import com.gamereleasetracker.repository.UserRepository;
import com.gamereleasetracker.service.GameUpdateService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
// @SpringBootTest
// @ActiveProfiles("test") // Ensures this test runs with the 'test' configuration
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest extends BaseIntegrationTest {

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

    @BeforeEach
    public void setUp() {
        // Ensures the "ROLE_USER" role exists, consistent with UserRepositoryIntegrationTest
        userRole = roleRepository.findByName(RoleType.ROLE_USER).orElseGet(() -> {
            Role newUserRole = new Role();
            newUserRole.setName(RoleType.ROLE_USER);
            return roleRepository.save(newUserRole);
        });
    }

    /**
     * Validates that when a user registers with valid credentials, a new user is created in the database,
     * and the API responds with HTTP status 201 (Created) along with the username in the response payload.
     * Test Steps:
     * 1. Constructs a valid UserCreateRequestDto with a unique username, email, and valid password.
     * 2. Sends a POST request to the /api/auth/register endpoint with the request payload in JSON format.
     * 3. Verifies that the response status is 201 and the response body contains the correct username.
     * 4. Confirms that the user is saved in the database with the correct username and an encoded password.
     *
     * @throws Exception if an error occurs during the test execution, such as failing to send the request or verify assertions.
     */
    @Test
    void registerUser_withValidCredentials_shouldCreateUserAndReturn201() throws Exception {
        UserCreateRequestDto createDto = new UserCreateRequestDto("newuser", "test@test", "password123", userRole.getName());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"));

        User savedUser = userRepository.findByUsername("newuser").orElseThrow();
        assertThat(savedUser).isNotNull();
        assertThat(passwordEncoder.matches("password123", savedUser.getPasswordHash())).isTrue();
    }

    /**
     * Verify that registering with a duplicate username results in an HTTP 409 (Conflict) response.
     * Test Steps:
     * 1. Create an existing test user with a specific username, email, and password.
     * 2. Construct a UserCreateRequestDto with the same username as the existing user
     *    to simulate a duplicate username conflict.
     * 3. Send a POST request to the /api/auth/register endpoint with the request payload in JSON format.
     * 4. Verify that the response status is 409 (Conflict) to confirm that the API prevents
     *    registration with a duplicate username.
     *
     * @throws Exception if an error occurs during the test execution, such as sending the request
     *                   or verifying the assertions.
     */
    @Test
    void registerUser_withDuplicateUsername_shouldReturn409() throws Exception {
        // Arrange: create an existing user
        createTestUser("existinguser","test@test",  "password123");
        UserCreateRequestDto createDto = new UserCreateRequestDto("existinguser", "test@test", "password123", userRole.getName());

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isConflict());
    }

    /**
     * Ensures that when a user attempts to register with a blank username, the API
     * responds with an HTTP status of 400 (Bad Request).
     * Test Steps:
     * 1. Constructs a UserCreateRequestDto with a blank username, a valid email,
     *    and a valid password.
     * 2. Sends a POST request to the `/api/auth/register` endpoint with the request payload
     *    in JSON format.
     * 3. Verifies that the API response status is 400, indicating that the request is invalid
     *    due to the blank username.
     *
     * @throws Exception if an error occurs during test execution, such as request failure
     *                   or assertion failure.
     */
    @Test
    void registerUser_withBlankUsername_shouldReturn400() throws Exception {
        UserCreateRequestDto createDto = new UserCreateRequestDto("",  "test@test", "password123", userRole.getName());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Ensures that logging in with valid credentials returns an HTTP 200 (OK) status and a session cookie.
     * Test Steps:
     * 1. Creates a test user with a valid username, email, and password using the `createTestUser` method.
     * 2. Constructs a `LoginRequestDto` with the created user's credentials and `rememberMe` set to false.
     * 3. Sends a POST request to the `/api/auth/login` endpoint with the login request payload in JSON format.
     * 4. Verifies that the response status is 200 (OK).
     * 5. Confirms that the response includes the correct username in the JSON payload.
     * 6. Checks that a session cookie (`SESSION`) is present in the response.
     * 7. Ensures that the "remember-me" cookie is not included in the response.
     *
     * @throws Exception if an error occurs during the test execution, such as failing to send the request
     *                   or verify assertions.
     */
    @Test
    void login_withValidCredentials_shouldReturn200AndSessionCookie() throws Exception {
        createTestUser("testuser", "test@test", "password");
        LoginRequestDto loginRequest = new LoginRequestDto("testuser", "password", false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(cookie().exists("SESSION"))
                .andExpect(cookie().doesNotExist("remember-me"));
    }

    /**
     * Verifies that logging in with valid credentials and the "remember me" option enabled
     * returns an HTTP status of 200 (OK) and sets both a session cookie and a "remember-me" cookie.
     * Test Steps:
     * 1. Creates a test user with a valid username, email, and password using the `createTestUser` method.
     * 2. Constructs a `LoginRequestDto` with the test user's credentials and the "rememberMe" parameter set to true.
     * 3. Sends a POST request to the `/api/auth/login` endpoint with the login request payload in JSON format.
     * 4. Confirms that the response status is 200 (OK).
     * 5. Verifies that the response includes both a session cookie (`SESSION`) and a "remember-me" cookie.
     * 6. Ensures that the persistent login repository contains exactly one entry for the logged-in user with the correct username.
     *
     * @throws Exception if an error occurs during the test execution, such as sending the request or verifying assertions.
     */
    @Test
    void login_withValidCredentialsAndRememberMe_shouldReturn200AndRememberMeCookie() throws Exception {
        createTestUser("testuser", "test@test", "password");
        LoginRequestDto loginRequest = new LoginRequestDto("testuser", "password", true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("SESSION"))
                .andExpect(cookie().exists("remember-me"));

        assertThat(persistentLoginRepository.findAll()).hasSize(1);
        assertThat(persistentLoginRepository.findAll().get(0).getUsername()).isEqualTo("testuser");
    }

    /**
     * Verifies that attempting to log in with a valid username but an incorrect password
     * results in an HTTP 401 (Unauthorized) response.
     * Test Steps:
     * 1. Creates a test user with a valid username, email, and password using the `createTestUser` method.
     * 2. Constructs a `LoginRequestDto` with the test user's username and an incorrect password.
     * 3. Sends a POST request to the `/api/auth/login` endpoint with the login request payload in JSON format.
     * 4. Confirms that the API responds with a 401 (Unauthorized) status, indicating invalid login credentials.
     *
     * @throws Exception if an error occurs while sending the request or asserting the response.
     */
    @Test
    void login_withInvalidPassword_shouldReturn401() throws Exception {
        createTestUser("testuser", "test@test", "password");
        LoginRequestDto loginRequest = new LoginRequestDto("testuser", "wrongpassword", false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Ensures that when a user with valid credentials is authenticated via the session endpoint,
     * the API responds with an HTTP 200 (OK) status and returns the user's data in the response body.
     * Test Steps:
     * 1. Creates a test user with valid credentials using the `createTestUser` method.
     * 2. Constructs a `LoginRequestDto` with the created user's credentials and `rememberMe` set to false.
     * 3. Sends a POST request to the `/api/auth/login` endpoint with the login request payload in JSON format.
     * 4. Verifies that the login response includes a session cookie (`SESSION`).
     * 5. Sends a GET request to the `/api/auth/session` endpoint using the obtained session cookie.
     * 6. Confirms that the response status is 200 (OK).
     * 7. Verifies that the returned JSON payload contains the correct username of the authenticated user.
     *
     * @throws Exception if an error occurs during test execution, such as sending requests
     *                   or verifying assertions.
     */
    @Test
    void getSessionUser_withAuthenticatedUser_shouldReturn200AndUserData() throws Exception {
        createTestUser("testuser", "test@test.com", "password");
        LoginRequestDto loginRequest = new LoginRequestDto("testuser", "password", false);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Instead of passing the MockHttpSession object, we pass the SESSION cookie,
        // which mimics a real browser client and works correctly with Spring Session + Redis.
        Cookie sessionCookie = loginResult.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).isNotNull();

        mockMvc.perform(get("/api/auth/session").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    /**
     * Validates that accessing the `/api/auth/session` endpoint without authentication
     * results in an HTTP 401 (Unauthorized) response.
     * Test Steps:
     * 1. Sends a GET request to the `/api/auth/session` endpoint without providing any authentication details.
     * 2. Verifies that the response status is 401, indicating that access to the session information is unauthorized.
     *
     * @throws Exception if an error occurs during the test execution, such as failing to send the request
     *                   or verify assertions.
     */
    @Test
    void getSessionUser_withUnauthenticatedUser_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isUnauthorized());
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
    private void createTestUser(String username, String email, String password) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(userRole);
        userRepository.save(user);
    }
}