package com.gamereleasetracker.controller;

import com.gamereleasetracker.dto.LoginRequestDto;
import com.gamereleasetracker.dto.UserCreateRequestDto;
import com.gamereleasetracker.dto.UserDto;
import com.gamereleasetracker.dto.UserRegistrationDto;
import com.gamereleasetracker.model.RoleType;
import com.gamereleasetracker.exception.DuplicateResourceException;
import com.gamereleasetracker.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController provides REST endpoints for authentication and user session management.
 * It includes functionalities for user login, registration, and session retrieval.
 * Behavioral logic such as authentication checks, session handling, and validation
 * are facilitated through injected service components.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final RememberMeServices rememberMeServices;

    public AuthController(UserService userService, AuthenticationManager authenticationManager, RememberMeServices rememberMeServices) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.rememberMeServices = rememberMeServices;
    }
    /**
     * Authenticates a user with a username and password via a JSON request body.
     * On successful authentication, a new stateful session is created and stored in Redis.
     * The session ID is returned to the client as the 'SESSION' cookie.
     * If the 'rememberMe' flag is true, a persistent 'remember-me' cookie is also returned.
     *
     * @param loginRequest The DTO containing the user's credentials and remember-me preference.
     * @param request The incoming HttpServletRequest, used to create a new session.
     * @param response The outgoing HttpServletResponse, used to set the remember-me cookie.
     * @return A ResponseEntity containing the authenticated user's details on success (200 OK),
     * or an empty body with a 401 Unauthorized status on failure.
     */
    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@Valid @RequestBody LoginRequestDto loginRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        try {
            // 1. Create an authentication token with the credentials from the request.
            // This token is not yet authenticated.
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password());

            // 2. Delegate the authentication attempt to the AuthenticationManager.
            // It will use UserDetailsService to find the user and the PasswordEncoder to validate the password.
            // If authentication fails, it throws an AuthenticationException.
            Authentication authentication = authenticationManager.authenticate(token);

            // 3. If authentication is successful, create a new SecurityContext.
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            // Populate the context with the fully authenticated Authentication object.
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // 4. Manually create a new HttpSession and store the SecurityContext in it.
            // This step makes the session stateful. Spring Session will persist this session to Redis,
            // and the client will receive the SESSION cookie.
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            // 5. Check if the user requested the "remember-me" functionality.
            if (loginRequest.rememberMe()) {
                // If so, manually trigger the RememberMeServices to generate a persistent token,
                // save it to the database, and set the 'remember-me' cookie on the response.
                rememberMeServices.loginSuccess(request, response, authentication);
            }

            // 6. Retrieve the authenticated principal (the user details) and fetch the corresponding DTO.
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UserDto userDto = userService.getUserByUsername(userDetails.getUsername());

            // 7. Return the user's data in the response body with a 200 OK status.
            return ResponseEntity.ok(userDto);

        } catch (AuthenticationException e) {
            // If authenticationManager.authenticate() throws an exception (e.g., bad credentials),
            // catch it here and return a 401 Unauthorized response.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Registers a new user with a username and password via a JSON request body.
     *
     * @param registrationDto The DTO containing the user's credentials.
     * @return A ResponseEntity containing the newly created user's details on success (201 Created),
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        try {
            // Map UserRegistrationDto to UserCreateDto
            UserCreateRequestDto createDto = new UserCreateRequestDto(
                    registrationDto.username(),
                    registrationDto.email(),
                    registrationDto.password(),
                    // Set the default role here
                    RoleType.ROLE_USER
            );
            // Delegate to the service layer with the new DTO
            UserDto newUserDto = userService.createUser(createDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(newUserDto);
        } catch (DuplicateResourceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /**
     * Retrieves the authenticated user's session details.
     * If the user is authenticated, their details are fetched and returned as a UserDto.
     * If no user is authenticated, a 401 Unauthorized status is returned.
     *
     * @param userDetails The authenticated user details provided by Spring Security's authentication system.
     * @return A ResponseEntity containing the authenticated user's UserDto on success (200 OK),
     * or an empty response with a 401 Unauthorized status if no user is authenticated.
     */
    @GetMapping("/session")
    public ResponseEntity<UserDto> getSessionUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            // User is authenticated, fetch their DTO directly from the user service.
            // The service handles the database lookup and mapping.
            UserDto userDto = userService.getUserByUsername(userDetails.getUsername());
            return ResponseEntity.ok(userDto);
        }
        // No user is authenticated.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}

