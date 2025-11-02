package com.gamereleasetracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a user in the application.
 * This class is a JPA entity that maps to the 'users' table in the database.
 */

@Getter
@Setter
@ToString(exclude = {"role", "wishlistItems"})
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enableNotifications = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Managed automatically by the onUpdate() lifecycle callback.
    @Column(nullable = false)
    private Instant updatedAt;

    // --- Spring Security ---
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean accountNonExpired = true;

    @Column(nullable = false)
    private boolean credentialsNonExpired = true;

    @Column(nullable = false)
    private boolean accountNonLocked = true;
    // --- Spring Security ---

    // Many users can have the same role.
    @ManyToOne(fetch = FetchType.LAZY)
    // Specifies the foreign key column in the 'users' table.
    // 'name = "role_id"' sets the column name that links to the 'roles' table.
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // One user can have many items on their wishlist.
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WishlistItem> wishlistItems = new HashSet<>();

    // --- Lifecycle Callbacks ---

    /* Automatically sets the createdAt and updatedAt timestamps before the
    entity is first saved to the database. */
    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    /* Automatically updates the updatedAt timestamp before an existing
     entity is updated in the database. */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}