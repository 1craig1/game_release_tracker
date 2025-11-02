package com.gamereleasetracker.model;

import jakarta.persistence.Embeddable;
import java.io.Serializable;

/**
 * Represents the composite primary key for a WishlistItem.
 * This class combines a userId and a gameId to uniquely identify a
 * wishlist entry, ensuring a user can only add a specific game to their
 * wishlist once. It's used as an @Embeddable ID within the
 * WishlistItem entity.
 */

// Specifies that this class will be embedded as a component of another entity.
@Embeddable
/* record is a special, concise class in modern Java for creating immutable
data carriers. It automatically generates private final fields, a constructor,
getters, and the equals(), hashCode(), and toString() methods.
It's ideal for a composite key, which should not change once created. */
public record WishlistItemId(
        Long userId,
        Long gameId
/* JPA requires all primary key classes to be 'Serializable'.
This allows the ID object to be reliably converted into a stream of bytes,
so it can be stored, cached, or transferred. */
) implements Serializable {
    /* A default, no-argument constructor.
     While a 'record' automatically gets a constructor with all its fields,
     the JPA specification requires that embeddable classes have a public,
     no-argument constructor. This is for the persistence provider's internal use. */
    public WishlistItemId() {
        this(null, null);
    }
}