package com.gamereleasetracker.controller;

import com.gamereleasetracker.dto.PasswordUpdateDto;
import com.gamereleasetracker.dto.UserCreateRequestDto;
import com.gamereleasetracker.dto.UserDto;
import com.gamereleasetracker.dto.UserProfileUpdateRequestDto;
import com.gamereleasetracker.exception.DuplicateResourceException;
import com.gamereleasetracker.model.RoleType;
import com.gamereleasetracker.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;
    private final UserDetailsService userDetailsService;

    public UserController(UserService service, UserDetailsService userDetailsService) {
        this.service = service;
        this.userDetailsService = userDetailsService;
    }

    // GET /api/users/me Description: Retrieves the profile details of the currently logged-in user.
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build(); // Unauthorized if no user is logged in
        }
        UserDto userDto = service.getUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(userDto);
    }

    // PUT /api/users/me Description: Replaces the entire user profile with new data.
    @PutMapping("/me")
    public ResponseEntity<UserDto> updateCurrentUser(@AuthenticationPrincipal UserDetails userDetails,
                                                    @Valid @RequestBody UserProfileUpdateRequestDto updateRequest,
                                                    HttpServletRequest request) {
        // 1. Check if a user is authenticated at all.
        if (userDetails == null) {
            // If @AuthenticationPrincipal injects null, no user is logged in.
            return ResponseEntity.status(401).build(); // Unauthorized if no user is logged in
        }
        // 2. Security Check: Ensure the authenticated user is only updating themselves.
        // This compares the ID of the logged-in user with the ID specified in the request body.
        if (!service.getUserByUsername(userDetails.getUsername()).id().equals(updateRequest.id())) {
            // User is trying to update another user's profile, which is not allowed.
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden if trying to update another user
        }
        // 3. Delegate to the service layer to perform the actual update.
        UserDto updatedUser = service.updateUser(updateRequest);

        // 4. Update Security Context (if username changed):
        // If the username was part of the update, the old security principal is outdated.
        // We must refresh the Authentication object in the SecurityContext to avoid
        // issues on subsequent requests within the same session.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if an authentication context exists and if the username in the DTO is not null.
        if (authentication != null && updatedUser.username() != null) {
            // Reload the user details with the (potentially new) username.
            UserDetails refreshedPrincipal = userDetailsService.loadUserByUsername(updatedUser.username());

            // Create a new Authentication token with the refreshed principal (user details)
            // and the original credentials and authorities.
            UsernamePasswordAuthenticationToken newAuthentication =
                    new UsernamePasswordAuthenticationToken(
                            refreshedPrincipal,
                            authentication.getCredentials(),
                            refreshedPrincipal.getAuthorities()
                    );

            // Create a new, empty security context and set the new authentication token.
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(newAuthentication);
            // Set the new context on the SecurityContextHolder for the current thread.
            SecurityContextHolder.setContext(context);

            // 5. Explicitly update the HTTP session.
            // This ensures the new SecurityContext is persisted in the session for
            // subsequent requests from the user.
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            }
        }
        // 6. Return the updated user data with a 200 OK status.
        return ResponseEntity.ok(updatedUser);
    }

    // PUT /api/{userId}/me/password Description: Update the user's password.
    @PutMapping("/me/password")
    public ResponseEntity<Void> updatePassword(@AuthenticationPrincipal UserDetails userDetails,
                                               @Valid @RequestBody PasswordUpdateDto passwordUpdateRequest) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }

        // Pass all data to the service layer to handle the logic
        Long id = service.getUserByUsername(userDetails.getUsername()).id();
        service.updateUserPassword(id, passwordUpdateRequest);

        return ResponseEntity.noContent().build(); // 204 No Content on success
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }
        service.deleteUser(service.getUserByUsername(userDetails.getUsername()).id());
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @DeleteMapping("/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')") // Added Security
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        
        Long currentAdminId = service.getUserByUsername(userDetails.getUsername()).id();
        if (currentAdminId.equals(userId)) {
            // Return 403 Forbidden if admin tries to delete themselves
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        service.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/id/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> viewUserById(@PathVariable Long userId) {
        UserDto userDto = service.getUserById(userId);
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/admin/username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> viewUserByUserName(@PathVariable String username) {
        UserDto userDto = service.getUserByUsername(username);
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> viewUsers() {
        List<UserDto> userDtos = service.getAllUsers();
        return ResponseEntity.ok(userDtos);
    }
    
    @PutMapping("/admin/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserRole(@PathVariable Long userId, 
                                            @RequestBody RoleType newRole,
                                            @AuthenticationPrincipal UserDetails userDetails) { 
        
        Long currentAdminId = service.getUserByUsername(userDetails.getUsername()).id();
        if (currentAdminId.equals(userId)) {
            // Return 403 Forbidden if admin tries to change their own role
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body("Admins cannot change their own role."); 
        }

        UserDto updatedUser = service.updateUserRole(userId, newRole);
        return ResponseEntity.ok(updatedUser);
    }

}
