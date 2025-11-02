package com.gamereleasetracker.dto;

import java.time.Instant;

public record NotificationDto(
        Long id,
        Long userId,
        Long gameId,
        String gameTitle,
        String gameCoverImageUrl,
        String message,
        boolean isRead,
        Instant createdAt
) {}