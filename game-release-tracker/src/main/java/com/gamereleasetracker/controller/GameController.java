package com.gamereleasetracker.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gamereleasetracker.dto.GameDetailDto;
import com.gamereleasetracker.dto.GameSummaryDto;
import com.gamereleasetracker.model.GameStatus;
import com.gamereleasetracker.service.GameService;
import com.gamereleasetracker.service.GameUpdateService;

@RestController
@RequestMapping("/api")
public class GameController {

    private final GameService gameService;
    private final GameUpdateService gameUpdateService;

    public GameController(GameService gameService, GameUpdateService gameUpdateService) {
        this.gameService = gameService;
        this.gameUpdateService = gameUpdateService;
    }

     /** 
     * GET /api/games - comprehensive filtering endpoint
     * Examples:
     * - GET /api/games (all games)
     * - GET /api/games?genres=Action,RPG (multiple genres)
     * - GET /api/games?platforms=PC,PlayStation (multiple platforms)  
     * - GET /api/games?status=RELEASED (filter by status)
     * - GET /api/games?search=mario (search by title)
     * - GET /api/games?date=2024-12-01 (games releasing after this date)
     * - GET /api/games?genres=Action&platforms=PC&status=UPCOMING (combined filters)
     */
    @GetMapping("/games")
    public ResponseEntity<List<GameSummaryDto>> getGames(
            @RequestParam(required = false) Set<String> genres,
            @RequestParam(required = false) Set<String> platforms,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate date) {
        
        GameStatus gameStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                gameStatus = GameStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build(); // Invalid status value
            }
        }
        // Use the service's comprehensive filtering method
        List<GameSummaryDto> games = gameService.getFilteredGames(genres, platforms, gameStatus, search, date);
        return ResponseEntity.ok(games);
    }

    /** GET /api/games/{id} — game detail */
    @GetMapping("/games/{id}")
    public ResponseEntity<GameDetailDto> viewGame(@PathVariable Long id) {
        return gameService.getGameById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
