package com.gamereleasetracker.dto;

import jakarta.validation.constraints.*;

public record PreorderLinkDto(
        Long id,
        Long gameId,
        String storeName,
        String url
) {}