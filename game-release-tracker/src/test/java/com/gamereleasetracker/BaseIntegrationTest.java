package com.gamereleasetracker;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * An abstract base class for all integration tests in the application.
 * This class sets up the core testing environment by:
 * 1.  Loading the full Spring application context using @SpringBootTest.
 * 2.  Activating the 'test' Spring profile to load test-specific configurations
 * (e.g., from application-test.properties).
 * 3.  Importing the TestcontainerConfiguration, which provides the necessary
 * Docker container beans (like the PostgreSQL database) for the tests.
 * Individual test classes should extend this class to inherit the pre-configured
 * testing environment.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainerConfiguration.class)
public abstract class BaseIntegrationTest {

    @BeforeEach
    public void setUp() {
        // This method can be used for common setup logic
        // that needs to run before each test method in subclasses.
    }
}