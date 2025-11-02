package com.gamereleasetracker;

import com.gamereleasetracker.dto.UserCreateRequestDto;
import com.gamereleasetracker.dto.UserDto;
import com.gamereleasetracker.dto.PasswordUpdateDto;
import com.gamereleasetracker.dto.UserProfileUpdateRequestDto;
import com.gamereleasetracker.exception.DuplicateResourceException;
import com.gamereleasetracker.exception.NotFoundException;
import com.gamereleasetracker.exception.UserNotFoundException;
import com.gamereleasetracker.exception.InvalidPasswordException;
import com.gamereleasetracker.model.Role;
import com.gamereleasetracker.model.RoleType;
import com.gamereleasetracker.model.User;
import com.gamereleasetracker.repository.RoleRepository;
import com.gamereleasetracker.repository.UserRepository;
import com.gamereleasetracker.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ActiveProfiles("test") // Ensures this test runs with the 'test' configuration
@ExtendWith(MockitoExtension.class) // Initializes Mockito
class UserServiceUnitTest {

    @Mock // Creates a mock instance of this dependency
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks // Creates an instance of UserService and injects the mocks into it
    private UserService userService;

    private User user;
    private UserCreateRequestDto userCreateRequestDto;
    private Role userRole;

    @BeforeEach
    void setUp() {
        // Common setup for tests
        userCreateRequestDto = new UserCreateRequestDto("testuser", "test@example.com", "rawPassword123", RoleType.ROLE_USER);
        userRole = new Role();
        userRole.setName(RoleType.ROLE_USER);
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedPassword");
        user.setEnableNotifications(true);
        user.setRole(userRole);
    }

    //region === Get All Users Tests ===
    @Test
    void testGetAllUsers_ShouldReturnUserDtoList() {
        // --- Setup ---
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUsername("user2");
        anotherUser.setEmail("user2@example.com");
        anotherUser.setRole(userRole);
        when(userRepository.findAll()).thenReturn(List.of(user, anotherUser));

        // --- Action ---
        List<UserDto> result = userService.getAllUsers();

        // --- Assertion ---
        assertThat(result).hasSize(2);
        assertThat(result.get(0).username()).isEqualTo("testuser");
        assertThat(result.get(1).username()).isEqualTo("user2");
    }

    @Test
    void testGetAllUsers_ShouldReturnEmptyList() {
        // --- Setup ---
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // --- Action ---
        List<UserDto> result = userService.getAllUsers();

        // --- Assertion ---
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }
    //endregion

    //region === Get User By Username Tests ===
    @Test
    void testGetUserByUsername_UserExists_ShouldReturnUserDto() {
        // --- Setup ---
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // --- Action ---
        UserDto result = userService.getUserByUsername("testuser");

        // --- Assertion ---
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(user.getId());
        assertThat(result.username()).isEqualTo(user.getUsername());
    }

    @Test
    void testGetUserByUsername_UserDoesNotExist_ShouldThrowException() {
        // --- Setup ---
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // --- Action & Assertion ---
        assertThrows(UserNotFoundException.class, () -> userService.getUserByUsername("nonexistent"));
    }
    //endregion

    //region === Get User By ID Tests ===
    @Test
    void testGetUserById_UserExists_ShouldReturnUserDto() {
        // --- Setup ---
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // --- Action ---
        UserDto result = userService.getUserById(1L);

        // --- Assertion ---
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(user.getId());
    }

    @Test
    void testGetUserById_UserDoesNotExist_ShouldThrowException() {
        // --- Setup ---
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // --- Action & Assertion ---
        assertThrows(UserNotFoundException.class, () -> userService.getUserById(99L));
    }
    //endregion

    //region === Create User Tests ===
    @Test
    void testCreateUser_Success() {
        // --- Setup ---
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleType.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        // "When the save method is called with a User object..."
        when(userRepository.save(any(User.class)))
                // "...run this code to figure out the answer."
                // Use `thenAnswer` to return the saved entity, simulating the repository's behavior
                .thenAnswer(invocation -> {
                    // Get the User object that was passed in (the first argument).
                    User userArgument = invocation.getArgument(0);
                    // Return that exact same object.
                    return userArgument;
                });

        // --- Action ---
        UserDto result = userService.createUser(userCreateRequestDto);

        // --- Assertion ---
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo(userCreateRequestDto.username());
        assertThat(result.email()).isEqualTo(userCreateRequestDto.email());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testCreateUser_UsernameExists_ShouldThrowException() {
        // --- Setup ---
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // --- Action & Assertion ---
        assertThrows(DuplicateResourceException.class, () -> userService.createUser(userCreateRequestDto));
    }

    @Test
    void testCreateUser_EmailExists_ShouldThrowException() {
        // --- Setup ---
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // --- Action & Assertion ---
        assertThrows(DuplicateResourceException.class, () -> userService.createUser(userCreateRequestDto));
    }

    @Test
    void testCreateUser_RoleNotFound_ShouldThrowException() {
        // --- Setup ---
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleType.ROLE_USER)).thenReturn(Optional.empty());

        // --- Action & Assertion ---
        assertThrows(NotFoundException.class, () -> userService.createUser(userCreateRequestDto));
    }

    @Test
    void testEncodePasswordWhenCreateUser() {
        // --- Setup ---
        String rawPassword = userCreateRequestDto.password();
        String hashedPassword = "hashedPasswordValue";
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleType.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // --- Action ---
        userService.createUser(userCreateRequestDto);

        // --- Assertion ---
        verify(passwordEncoder, times(1)).encode(rawPassword);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo(hashedPassword);
    }
    //endregion

    //region === Update User Tests ===
    @Test
    void testUpdateUser_Success() {
        // --- Setup ---
        UserProfileUpdateRequestDto updateDto = new UserProfileUpdateRequestDto(1L, "newUsername", "new@example.com", false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("newUsername")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // --- Action ---
        UserDto result = userService.updateUser(updateDto);

        // --- Assertion ---
        assertThat(result.username()).isEqualTo("newUsername");
        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.enableNotifications()).isFalse();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("newUsername");
    }

    @Test
    void testUpdateUser_UserNotFound_ShouldThrowException() {
        // --- Setup ---
        UserProfileUpdateRequestDto updateDto = new UserProfileUpdateRequestDto(99L, "any", "any@any.com", true);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // --- Action & Assertion ---
        assertThrows(UserNotFoundException.class, () -> userService.updateUser(updateDto));
    }

    @Test
    void testUpdateUser_EmailIsTaken_ShouldThrowException() {
        // --- Setup ---
        UserProfileUpdateRequestDto updateDto = new UserProfileUpdateRequestDto(1L, "testuser", "taken@example.com", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        // --- Action & Assertion ---
        assertThrows(DuplicateResourceException.class, () -> userService.updateUser(updateDto));
    }

    @Test
    void testUpdateUser_UsernameIsTaken_ShouldThrowException() {
        // --- Setup ---
        UserProfileUpdateRequestDto updateDto = new UserProfileUpdateRequestDto(1L, "takenUsername", "new@example.com", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("takenUsername")).thenReturn(true);

        // --- Action & Assertion ---
        assertThrows(DuplicateResourceException.class, () -> userService.updateUser(updateDto));
    }
    //endregion

    //region === Change Password Tests ===
    @Test
    void testEncodePasswordWhenChangePassword() {
        // --- Setup ---
        Long userId = 1L;
        String newRawPassword = "newSecurePassword456";
        String newHashedPassword = "newHashedPasswordValue";
        PasswordUpdateDto passwordUpdateDto = new PasswordUpdateDto("oldPassword", newRawPassword, newRawPassword);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode(newRawPassword)).thenReturn(newHashedPassword);

        // --- Action ---
        userService.updateUserPassword(userId, passwordUpdateDto);

        // --- Assertion ---
        verify(passwordEncoder, times(1)).encode(newRawPassword);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo(newHashedPassword);
    }

    @Test
    void testChangeUserPassword_UserNotFound_ShouldThrowException() {
        // --- Setup ---
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        PasswordUpdateDto passwordUpdateDto = new PasswordUpdateDto("anyOldPassword", "anyNewPassword", "anyNewPassword");
        // --- Action & Assertion ---
        assertThrows(UserNotFoundException.class, () -> userService.updateUserPassword(99L, passwordUpdateDto));
    }

    @Test
    void testChangeUserPassword_IncorrectOldPassword_ShouldThrowException() {
        // --- Setup ---
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongOldPassword", "hashedPassword")).thenReturn(false);
        PasswordUpdateDto passwordUpdateDto = new PasswordUpdateDto("wrongOldPassword", "newPassword", "newPassword");

        // --- Action & Assertion ---
        assertThrows(InvalidPasswordException.class, () -> userService.updateUserPassword(userId, passwordUpdateDto));
    }

    @Test
    void testChangeUserPassword_NewPasswordsDoNotMatch_ShouldThrowException() {
        // --- Setup ---
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        PasswordUpdateDto passwordUpdateDto = new PasswordUpdateDto("oldPassword", "newPassword1", "newPassword2");

        // --- Action & Assertion ---
        assertThrows(InvalidPasswordException.class, () -> userService.updateUserPassword(userId, passwordUpdateDto));
    }
    //endregion

    //region === Delete User Tests ===
    @Test
    void testDeleteUser_Success() {
        // --- Setup ---
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);
        // `deleteById` returns void, so we can use `doNothing()`
        doNothing().when(userRepository).deleteById(userId);

        // --- Action ---
        userService.deleteUser(userId);

        // --- Assertion ---
        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    void testDeleteUser_UserNotFound_ShouldThrowException() {
        // --- Setup ---
        Long userId = 99L;
        when(userRepository.existsById(userId)).thenReturn(false);

        // --- Action & Assertion ---
        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(userId));
        // Verify delete was never called
        verify(userRepository, never()).deleteById(anyLong());
    }
    //endregion
}
