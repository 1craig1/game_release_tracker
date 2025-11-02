package com.gamereleasetracker.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank String username,
        @NotBlank String password,
        boolean rememberMe
) {}