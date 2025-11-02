package com.gamereleasetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@SpringBootApplication
@EnableRedisHttpSession // This annotation enables Redis as the session store
@EnableScheduling // Enable scheduling for periodic tasks
public class GameReleaseTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameReleaseTrackerApplication.class, args);
    }

}
