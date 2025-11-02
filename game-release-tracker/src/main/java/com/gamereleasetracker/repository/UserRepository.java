package com.gamereleasetracker.repository;

import com.gamereleasetracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their unique username.
     *
     * @param username The username to search for.
     * @return An Optional containing the found user, or an empty Optional if not found.
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by their unique email address.
     *
     * @param email The email to search for.
     * @return An Optional containing the found user, or an empty Optional if not found.
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user with the given username already exists.
     *
     * @param username The username to check.
     * @return true if the username is already taken, false otherwise.
     */
    Boolean existsByUsername(String username);

    /**
     * Checks if a user with the given email already exists.
     *
     * @param email The email to check.
     * @return true if the email is already taken, false otherwise.
     */
    Boolean existsByEmail(String email);
}