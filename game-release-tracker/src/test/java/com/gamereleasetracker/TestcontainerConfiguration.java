package com.gamereleasetracker;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Provides Testcontainers-managed beans for integration tests.
 * This configuration class is responsible for creating and configuring the Docker containers
 * required for the test environment. By defining the container as a Spring @Bean, we ensure
 * it is part of the Spring context's lifecycle, guaranteeing it starts before the application
 * attempts to connect to it.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainerConfiguration {

    /**
     * Creates and configures the PostgreSQL container bean.
     * The @ServiceConnection annotation automatically configures the application's
     * DataSource properties (URL, username, password) to connect to this container,
     * overriding any properties defined in application-test.properties.
     * The container is set to be reusable across test runs for better performance.
     *
     * @return The configured PostgreSQLContainer instance.
     */
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:15-alpine")
                .withReuse(true);
    }
}