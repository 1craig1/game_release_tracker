package com.gamereleasetracker.service;

import com.gamereleasetracker.dto.UserCreateRequestDto;
import com.gamereleasetracker.dto.UserDto;
import com.gamereleasetracker.dto.PasswordUpdateDto;
import com.gamereleasetracker.dto.UserProfileUpdateRequestDto;
import com.gamereleasetracker.exception.DuplicateResourceException;
import com.gamereleasetracker.exception.InvalidPasswordException;
import com.gamereleasetracker.exception.UserNotFoundException;
import com.gamereleasetracker.exception.NotFoundException;
import com.gamereleasetracker.model.Role;
import com.gamereleasetracker.model.RoleType;
import com.gamereleasetracker.model.User;
import com.gamereleasetracker.repository.RoleRepository;
import com.gamereleasetracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Retrieves all users and maps them to UserDto objects.
     * @return a list of UserDtos representing all users.
     */
    @Transactional(readOnly = true) // Optimization for methods that only read data
    public List<UserDto> getAllUsers() {
        // Retrieve all users from DB
        List<User> users = userRepository.findAll();
        List<UserDto> userDtos = new ArrayList<>();

        for (User user : users) {
            // For each user, call the mapping function to convert it to a DTO.
            UserDto dto = this.convertToUserDto(user);
            userDtos.add(dto);
        }
        return userDtos;
    }

    /**
     * Retrieves a user by their username.
     * @param username A username.
     * @return A UserDto to avoid exposing the User entity.
     * @throws UserNotFoundException If no user is found.
     */
    @Transactional(readOnly = true)
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
        return convertToUserDto(user);
    }

    /**
     * Retrieves a user by their ID.
     * @param id The user's ID.
     * @return A UserDto to avoid exposing the User entity.
     * @throws UserNotFoundException If no user is found.
     */
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        return convertToUserDto(user);
    }

    /**
     * Creates a new user.
     * @param userDto A UserCreateRequestDto.
     * @return The created user as a UserDto.
     * @throws DuplicateResourceException If username or email already exists.
     * @throws RuntimeException If default user role is not in the database.
     */
    @Transactional
    public UserDto createUser(UserCreateRequestDto userDto) {
        // Check for duplicates
        if (userRepository.existsByUsername(userDto.username())) {
            throw new DuplicateResourceException("Username already exists: " + userDto.username());
        }
        if (userRepository.existsByEmail(userDto.email())) {
            throw new DuplicateResourceException("Email already exists: " + userDto.email());
        }
        // Find default role in DB
        Role userRole = roleRepository.findByName(userDto.role())
                        .orElseThrow(() -> new NotFoundException("Default user role not found in database."));
        // Create and configure new user
        User user = new User();
        user.setUsername(userDto.username());
        user.setEmail(userDto.email());
        user.setPasswordHash(passwordEncoder.encode(userDto.password()));
        user.setRole(userRole);
        // Save user in DB and return User DTO
        User savedUser = userRepository.save(user);
        return convertToUserDto(savedUser);
    }

    /**
     * Updates an existing user's details.
     * @param userDto The DTO containing the new details.
     * @return The updated user as a UserDto.
     * @throws UserNotFoundException If the user does not exist.
     * @throws DuplicateResourceException If the email already exists.
     */
    @Transactional
    public UserDto updateUser(UserProfileUpdateRequestDto userDto) {
        User user = userRepository.findById(userDto.id())
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userDto.id()));

        // Check if the email is being changed and if the new email is already taken
        if (userDto.email() != null && !userDto.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(userDto.email())) {
                throw new DuplicateResourceException("Email already exists: " + userDto.email());
            }
            user.setEmail(userDto.email());
        }

        // Check if the username is being changed and if the new username is already taken
        if (userDto.username() != null && !userDto.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(userDto.username())) {
                throw new DuplicateResourceException("Username already exists: " + userDto.username());
            }
            user.setUsername(userDto.username());
        }

        user.setEnableNotifications(userDto.enableNotifications());

        User updatedUser = userRepository.save(user);
        return convertToUserDto(updatedUser);
    }

    /**
     * Updates a user's password after verifying the current password and matching new passwords.
     * @param username The username of the user whose password is to be updated.
     * @param currentPassword The user's current password for verification.
     * @param newPassword The new password to set.
     * @param confirmPassword Confirmation of the new password to ensure they match.
     * @throws NotFoundException If the user does not exist.
     * @throws InvalidPasswordException If the current password is incorrect or new passwords do not match.
     */
    @Transactional
    public void updateUserPassword(Long userId, PasswordUpdateDto passwordUpdateDto) {
        String currentPassword = passwordUpdateDto.oldPassword();
        String newPassword = passwordUpdateDto.newPassword();
        String confirmPassword = passwordUpdateDto.confirmPassword();
        // 1. Find the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // 2. Verify the current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            // Throw a specific exception that can be handled by a global exception handler
            // to return a 403 Forbidden or 400 Bad Request.
            throw new InvalidPasswordException("Incorrect current password.");
        }

        // 3. Verify the new passwords match
        if (!newPassword.equals(confirmPassword)) {
            throw new InvalidPasswordException("New passwords do not match.");
        }

        // 4. Encode and set the new password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Deletes a user.
     * @param id The ID of the user to delete.
     * @throws UserNotFoundException If the user does not exist.
     */
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    public UserDto updateUserRole(Long userId, RoleType roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new NotFoundException("Role not found: " + roleName));
        user.setRole(role);
        return convertToUserDto(userRepository.save(user));
    }

    /**
     * Private helper method to map a User entity to a UserDto.
     * @param user A User entity.
     * @return A UserDto for that User.
     */
    private UserDto convertToUserDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnableNotifications(),
                user.getRole().getName()
        );
    }
}
