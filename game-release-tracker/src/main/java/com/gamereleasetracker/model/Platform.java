package com.gamereleasetracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a gaming platform (e.g., PC, PlayStation 5).
 */

@Getter
@Setter
@ToString(exclude = "games")
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "platforms")
public class Platform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, unique = true, nullable = false)
    private String name;

    // This is the "inverse" side of the relationship.
    // 'mappedBy = "platforms"' tells JPA that the Game entity is in charge of the relationship.
    @ManyToMany(mappedBy = "platforms")
    private Set<Game> games = new HashSet<>();
}