package com.gamereleasetracker.dto;

import com.gamereleasetracker.model.GameStatus;
import java.time.LocalDate;
import java.util.Set;

public record GameSummaryDto(
        Long id,
        String title,
        String description,
        String coverImageUrl,
        LocalDate releaseDate,
        GameStatus status,
        Set<GenreDto> genres,
        Set<PlatformDto> platforms,
        boolean mature
) {}
