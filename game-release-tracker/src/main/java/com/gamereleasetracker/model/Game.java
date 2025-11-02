package com.gamereleasetracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a game in the application.
 * This class is a JPA entity that maps to the 'games' table in the database.
 */

@Getter
@Setter
@ToString(exclude = {"preorderLinks", "wishlistItems", "platforms", "genres"})
@EqualsAndHashCode(of = "id")
@Entity
// This entity maps to the games table in the DB.
/* @Index tells JPA to create database indexes.
Indexes are critical for speeding up query performance for common operations like
searching by title, filtering by platform, and finding games by release date. */
@Table(name = "games", indexes = {
        @Index(name = "idx_games_title", columnList = "title"),
        @Index(name = "idx_games_status", columnList = "status"),
        @Index(name = "idx_games_release_date", columnList = "release_date")
})
public class Game {
    // PK of table
    @Id
    /* Configures the way the PK is generated.
    GenerationType.IDENTITY means DB is responsible for auto-incrementing the value */
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false) // 'nullable = false' means NOT NULL.
    private String title;

    @Column(columnDefinition = "TEXT") // explicitly define SQL data type as TEXT
    private String description;

    private String coverImageUrl;

    @Column(nullable = false)
    private LocalDate releaseDate;

    private String developer;
    private String publisher;

    @Column(unique = true)
    private String rawgGameSlug;

    // Tells JPA to store the enum value as a String (e.g., "UPCOMING") in the database.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameStatus status = GameStatus.UPCOMING; // Default value

    @Column(length = 20)
    private String ageRating;

    @Column(nullable = false)
    private boolean mature = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // One Game can have many PreorderLinks.
    /* mappedBy is used to point to the "owner" entity that holds the actual foreign key.
    "game" indicates that the 'game' field in PreorderLink owns the relationship (has @JoinColumn)
    This prevents the creation of an unnecessary join table.*/
    // cascade = CascadeType.ALL means operations (like save, delete) on a Game will cascade to its PreorderLinks.
    // orphanRemoval = true ensures that if a PreorderLink is removed from this set, it's also deleted from the database.
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PreorderLink> preorderLinks = new HashSet<>();

    // One Game can be in many WishlistItems.
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WishlistItem> wishlistItems = new HashSet<>();

    /* Many-to-many relationship between Game and Platform.
     @JoinTable specifies the intermediary table ("game_platforms") that links them.
     joinColumns: Defines the foreign key column ('game_id') in the
     join table that refers to the owner side of the relationship (Game entity)
     inverseJoinColumns: Defines the foreign key column ('platform_id') in the
     join table that refers to the inverse side of the relationship (Platform entity). */
    // FetchType.LAZY is a performance optimization.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "game_platforms",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "platform_id")
    )
    private Set<Platform> platforms = new HashSet<>();

    // Many-to-Many relationship with Genre
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "game_genres",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    // --- Lifecycle Callbacks ---
    /* This method is automatically called by JPA before an entity is first saved (persisted).
     It sets the initial createdAt and updatedAt timestamps. */
    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    /* This method is automatically called by JPA before an existing entity is updated.
    It updates the updatedAt timestamp. */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}