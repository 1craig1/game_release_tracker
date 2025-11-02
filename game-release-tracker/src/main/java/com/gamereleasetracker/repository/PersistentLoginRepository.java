package com.gamereleasetracker.repository;

import com.gamereleasetracker.model.PersistentLogin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PersistentLogin entity.
 * This repository provides the necessary database operations for managing
 * persistent "remember-me" tokens.
 */
@Repository
public interface PersistentLoginRepository extends JpaRepository<PersistentLogin, String> {

    /**
     * Finds a persistent login token by its series identifier.
     * @param seriesId The unique series ID.
     * @return The PersistentLogin entity.
     */
    PersistentLogin findBySeries(String seriesId);

    /**
     * Deletes all persistent tokens associated with a specific username.
     * This is typically called when a user logs out or changes their password.
     * @param username The user's username.
     */
    void deleteByUsername(String username);
}