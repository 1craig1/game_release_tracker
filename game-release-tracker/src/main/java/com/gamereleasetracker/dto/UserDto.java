package com.gamereleasetracker.dto;

import com.gamereleasetracker.model.RoleType;
// A secure representation of a user for API responses.
public record UserDto(
        Long id,
        String username,
        String email,
        boolean enableNotifications,
        RoleType role
) {}