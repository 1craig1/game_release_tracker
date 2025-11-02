package com.gamereleasetracker.model;

/**
 * Represents the release status of a game. Using an enum provides type safety
 * and ensures that the status can only be one of the predefined values.
 */
public enum GameStatus {
    UPCOMING,
    RELEASED,
    DELAYED,
    CANCELED
}