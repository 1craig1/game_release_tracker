package com.gamereleasetracker.dto;

import com.gamereleasetracker.model.RoleType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequestDto(
        // Ensures the username is not null and contains at least one non-whitespace character.
        @NotBlank(message = "Username cannot be blank")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        // Ensures the email is not null and is not just whitespace.
        @NotBlank(message = "Email cannot be blank")
        // Checks if the string follows a valid email format.
        @Email(message = "Please provide a valid email address")
        String email,

        // Ensures the password is not null or blank.
        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @NotNull(message = "Role cannot be null")
        RoleType role
) {
    // Compact constructor to set a default value
    public UserCreateRequestDto {
        if (role == null) {
            role = RoleType.ROLE_USER;
        }
    }
}