package com.gamereleasetracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

/** 
 * Represents a genre for each game (e.g., Action, RPG)
 */

@Getter
@Setter
@ToString(exclude = "games")
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "genres")
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, unique = true, nullable = false)
    private String name;

    @ManyToMany(mappedBy = "genres")
    private Set<Game> games = new HashSet<>();
}