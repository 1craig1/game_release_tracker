package com.gamereleasetracker.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordUpdateDto(
        @NotBlank String oldPassword,
        @NotBlank String newPassword,
        @NotBlank String confirmPassword
) {}