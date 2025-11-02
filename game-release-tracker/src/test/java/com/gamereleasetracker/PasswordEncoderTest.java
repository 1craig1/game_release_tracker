package com.gamereleasetracker;

import com.gamereleasetracker.service.GameUpdateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

// @SpringBootTest
// @ActiveProfiles("test")
class PasswordEncoderTest extends BaseIntegrationTest {

    @MockitoBean
    private GameUpdateService gameUpdateService;

    @Autowired // Inject the PasswordEncoder bean from the Spring Context
    private PasswordEncoder passwordEncoder;

    @Test
    void testPasswordEncoderEncryptsAndMatchesPassword() {
        // --- Setup ---
        String rawPassword = "password123";

        // --- Action ---
        String hashedPassword = passwordEncoder.encode(rawPassword);

        // --- Assertions ---
        // Raw password is not the same as the hashed one
        assertThat(rawPassword).isNotEqualTo(hashedPassword);

        // Encoder can match the raw password to the hashed one
        assertThat(passwordEncoder.matches(rawPassword, hashedPassword)).isTrue();

        // Wrong password does not match
        assertThat(passwordEncoder.matches("wrong-password", hashedPassword)).isFalse();

        // Encoding the same string twice results in two different hashes due to the random salt.
        String secondHashedPassword = passwordEncoder.encode(rawPassword);
        assertThat(hashedPassword).isNotEqualTo(secondHashedPassword);
        assertThat(passwordEncoder.matches(rawPassword, secondHashedPassword)).isTrue();
    }
}