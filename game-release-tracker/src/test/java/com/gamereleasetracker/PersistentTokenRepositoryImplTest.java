package com.gamereleasetracker;

import com.gamereleasetracker.model.PersistentLogin;
import com.gamereleasetracker.repository.PersistentLoginRepository;
import com.gamereleasetracker.service.PersistentTokenRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the PersistentTokenRepositoryImpl service.
 * This class tests the service's logic in isolation by mocking the PersistentLoginRepository.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PersistentTokenRepositoryImplTest {

    @Mock
    private PersistentLoginRepository persistentLoginRepository;

    @InjectMocks
    private PersistentTokenRepositoryImpl persistentTokenService;

    @Test
    void createNewToken_ShouldCorrectlyMapAndSaveToken() {
        // Arrange
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS); 
        PersistentRememberMeToken token = new PersistentRememberMeToken(
                "testuser",
                "series123",
                "tokenValue456",
                Date.from(now)
        );

        // Act
        persistentTokenService.createNewToken(token);

        // Assert
        // Capture the argument that was passed to the save method
        ArgumentCaptor<PersistentLogin> persistentLoginCaptor = ArgumentCaptor.forClass(PersistentLogin.class);
        verify(persistentLoginRepository).save(persistentLoginCaptor.capture());

        PersistentLogin capturedLogin = persistentLoginCaptor.getValue();

        // Verify that the mapping was correct
        assertNotNull(capturedLogin);
        assertEquals("testuser", capturedLogin.getUsername());
        assertEquals("series123", capturedLogin.getSeries());
        assertEquals("tokenValue456", capturedLogin.getToken());
        assertEquals(now, capturedLogin.getLastUsed());
    }

    @Test
    void updateToken_WhenTokenExists_ShouldUpdateAndSaveChanges() {
        // Arrange
        String series = "series123";
        Instant originalDate = Instant.now().minus(1, ChronoUnit.DAYS);
        PersistentLogin existingLogin = new PersistentLogin();
        existingLogin.setSeries(series);
        existingLogin.setUsername("testuser");
        existingLogin.setToken("oldTokenValue");
        existingLogin.setLastUsed(originalDate);

        // Mock the repository to return the existing token
        when(persistentLoginRepository.findBySeries(series)).thenReturn(existingLogin);

        String newTokenValue = "newTokenValue789";
        Date newLastUsedDate = new Date();
        Instant newLastUsedInstant = newLastUsedDate.toInstant();

        // Act
        persistentTokenService.updateToken(series, newTokenValue, newLastUsedDate);

        // Assert
        // Capture the entity passed to save() to verify its contents
        ArgumentCaptor<PersistentLogin> persistentLoginCaptor = ArgumentCaptor.forClass(PersistentLogin.class);
        verify(persistentLoginRepository).save(persistentLoginCaptor.capture());

        PersistentLogin savedLogin = persistentLoginCaptor.getValue();
        assertEquals(newTokenValue, savedLogin.getToken());
        assertEquals(newLastUsedInstant, savedLogin.getLastUsed());
    }

    @Test
    void updateToken_WhenTokenDoesNotExist_ShouldDoNothing() {
        // Arrange
        String series = "nonExistentSeries";
        // Mock the repository to return null, simulating a token that is not found
        when(persistentLoginRepository.findBySeries(series)).thenReturn(null);

        // Act
        persistentTokenService.updateToken(series, "someToken", new Date());

        // Assert
        // Verify that the save method was never called
        verify(persistentLoginRepository, never()).save(any(PersistentLogin.class));
    }

    @Test
    void getTokenForSeries_WhenTokenExists_ShouldReturnMappedToken() {
        // Arrange
        String seriesId = "series123";
        Instant lastUsed = Instant.now();
        PersistentLogin persistentLogin = new PersistentLogin();
        persistentLogin.setUsername("testuser");
        persistentLogin.setSeries(seriesId);
        persistentLogin.setToken("tokenValue456");
        persistentLogin.setLastUsed(lastUsed);
        when(persistentLoginRepository.findBySeries(seriesId)).thenReturn(persistentLogin);

        // Act
        PersistentRememberMeToken resultToken = persistentTokenService.getTokenForSeries(seriesId);

        // Assert
        assertNotNull(resultToken);
        assertEquals("testuser", resultToken.getUsername());
        assertEquals(seriesId, resultToken.getSeries());
        assertEquals("tokenValue456", resultToken.getTokenValue());
        assertEquals(Date.from(lastUsed), resultToken.getDate());
    }

    @Test
    void getTokenForSeries_WhenTokenDoesNotExist_ShouldReturnNull() {
        // Arrange
        String seriesId = "nonExistentSeries";
        when(persistentLoginRepository.findBySeries(seriesId)).thenReturn(null);

        // Act
        PersistentRememberMeToken resultToken = persistentTokenService.getTokenForSeries(seriesId);

        // Assert
        assertNull(resultToken);
    }

    @Test
    void removeUserTokens_ShouldCallDeleteByUsername() {
        // Arrange
        String username = "testuser";

        // Act
        persistentTokenService.removeUserTokens(username);

        // Assert
        // Verify that the repository's delete method was called exactly once with the correct username
        verify(persistentLoginRepository, times(1)).deleteByUsername(username);
    }
}