package com.gamereleasetracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    // Create the RestTemplate for API calls
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
