package com.gamereleasetracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating user profile information.
 */
public record UserProfileUpdateRequestDto(
        @NotNull(message = "User id is required")
        Long id,

        @NotBlank(message = "Username cannot be blank")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Please provide a valid email address")
        String email,

        boolean enableNotifications
) {}
