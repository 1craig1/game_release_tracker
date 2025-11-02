package com.gamereleasetracker;

import com.gamereleasetracker.service.GameUpdateService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// @SpringBootTest
// @ActiveProfiles("test")
class GameReleaseTrackerApplicationTests extends BaseIntegrationTest {

    @MockitoBean
    private GameUpdateService gameUpdateService;

    @Test
    void contextLoads() {
    }

}
