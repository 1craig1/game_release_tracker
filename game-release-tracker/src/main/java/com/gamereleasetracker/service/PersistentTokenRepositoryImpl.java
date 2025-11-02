package com.gamereleasetracker.service;

import com.gamereleasetracker.model.PersistentLogin;
import com.gamereleasetracker.repository.PersistentLoginRepository;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * A JPA-based implementation of Spring Security's PersistentTokenRepository.
 * This service class handles the logic for creating, updating, retrieving, and deleting
 * "remember-me" tokens from the database using our PersistentLoginRepository.
 * It takes generic requests from Spring Security (e.g., "create a new token") and
 * translates them into specific actions for our JPA repository
 */
@Service
public class PersistentTokenRepositoryImpl implements PersistentTokenRepository {

    private final PersistentLoginRepository persistentLoginRepository;

    public PersistentTokenRepositoryImpl(PersistentLoginRepository persistentLoginRepository) {
        this.persistentLoginRepository = persistentLoginRepository;
    }

    /**
     * Creates a new persistent token in the database.
     * @param token The token to be created
     */
    @Override
    @Transactional
    public void createNewToken(PersistentRememberMeToken token) {
        PersistentLogin persistentLogin = new PersistentLogin();
        persistentLogin.setUsername(token.getUsername());
        persistentLogin.setSeries(token.getSeries());
        persistentLogin.setToken(token.getTokenValue());
        persistentLogin.setLastUsed(token.getDate().toInstant());
        this.persistentLoginRepository.save(persistentLogin);
    }

    /**
     * Updates an existing persistent token in the database.
     * @param series The series of the token to be updated
     * @param tokenValue The new token value
     * @param lastUsed The date the token was last used
     */
    @Override
    @Transactional
    public void updateToken(String series, String tokenValue, Date lastUsed) {
        PersistentLogin persistentLogin = this.persistentLoginRepository.findBySeries(series);
        if (persistentLogin != null) {
            persistentLogin.setToken(tokenValue);
            persistentLogin.setLastUsed(lastUsed.toInstant());
            this.persistentLoginRepository.save(persistentLogin);
        }
    }

    /**
     * Retrieves a persistent token by its series ID.
     * @param seriesId The series ID of the token to retrieve
     * @return The PersistentRememberMeToken object for the specified series ID
     */
    @Override
    @Transactional(readOnly = true)
    public PersistentRememberMeToken getTokenForSeries(String seriesId) {
        PersistentLogin persistentLogin = this.persistentLoginRepository.findBySeries(seriesId);
        if (persistentLogin != null) {
            return new PersistentRememberMeToken(
                    persistentLogin.getUsername(),
                    persistentLogin.getSeries(),
                    persistentLogin.getToken(),
                    Date.from(persistentLogin.getLastUsed())
            );
        }
        return null;
    }

    /**
     * Removes all persistent tokens for a given user.
     * @param username The username of the user whose tokens should be removed
     */
    @Override
    @Transactional
    public void removeUserTokens(String username) {
        this.persistentLoginRepository.deleteByUsername(username);
    }
}
