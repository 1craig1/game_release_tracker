package com.gamereleasetracker.dto;

import com.gamereleasetracker.model.GameStatus;
import com.gamereleasetracker.model.Genre;
import com.gamereleasetracker.model.Platform;
import com.gamereleasetracker.model.PreorderLink;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;


// A complete representation of a game for its detail page.
public record GameDetailDto(
        Long id,
        String title,
        String description,
        String coverImageUrl,
        LocalDate releaseDate,
        String developer,
        String publisher,
        String rawgGameSlug,
        GameStatus status,
        String ageRating,
        boolean mature,
        Set<GenreDto> genres,
        Set<PlatformDto> platforms,
        Set<PreorderLinkDto> preorderLinks
) {}