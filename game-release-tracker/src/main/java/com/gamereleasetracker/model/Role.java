package com.gamereleasetracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a user role in the application.
 * This class is a JPA entity that maps to the 'roles' table in the database.
 */

@Getter
@Setter
@ToString(exclude = "users")
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING) // Store the enum as a string in the DB
    // 'unique = true' ensures that every role name in the database is unique.
    @Column(length = 20, unique = true, nullable = false)
    private RoleType name;

    // One role can be associated with many users.
    @OneToMany(mappedBy = "role")
    private Set<User> users = new HashSet<>();

}