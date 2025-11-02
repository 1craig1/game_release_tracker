package com.gamereleasetracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamereleasetracker.dto.GameRequestDto;
import com.gamereleasetracker.model.Game;
import com.gamereleasetracker.model.GameStatus;
import com.gamereleasetracker.model.Genre;
import com.gamereleasetracker.model.Platform;
import com.gamereleasetracker.repository.GameRepository;
import com.gamereleasetracker.repository.GenreRepository;
import com.gamereleasetracker.repository.PlatformRepository;
import com.gamereleasetracker.service.GameUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;


import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @SpringBootTest
// @ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class GameControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PlatformRepository platformRepository;

    @Autowired
    private GenreRepository genreRepository;

    @MockitoBean
    private GameUpdateService gameUpdateService;

    private Game game1, game2, game3;

    @BeforeEach
    public void setUp() {
        Genre rpgGenre = new Genre();
        rpgGenre.setName("RPG");
        genreRepository.save(rpgGenre);

        Genre actionGenre = new Genre();
        actionGenre.setName("Action");
        genreRepository.save(actionGenre);

        Platform pcPlatform = new Platform();
        pcPlatform.setName("PC");
        platformRepository.save(pcPlatform);

        Platform ps5Platform = new Platform();
        ps5Platform.setName("PS5");
        platformRepository.save(ps5Platform);

        game1 = new Game();
        game1.setTitle("Elden Ring");
        game1.setRawgGameSlug("elden-ring");
        game1.setStatus(GameStatus.RELEASED);
        game1.setReleaseDate(LocalDate.of(2022, 2, 25));
        game1.setDeveloper("FromSoftware");
        game1.setGenres(new HashSet<>(Set.of(rpgGenre, actionGenre)));
        game1.setPlatforms(new HashSet<>(Set.of(pcPlatform, ps5Platform)));

        game2 = new Game();
        game2.setTitle("Starfield");
        game2.setRawgGameSlug("starfield");
        game2.setStatus(GameStatus.RELEASED);
        game2.setReleaseDate(LocalDate.of(2023, 9, 6));
        game2.setDeveloper("Bethesda");
        game2.setGenres(new HashSet<>(Set.of(rpgGenre)));
        game2.setPlatforms(new HashSet<>(Set.of(pcPlatform)));

        game3 = new Game();
        game3.setTitle("Final Fantasy VII Rebirth");
        game3.setRawgGameSlug("final-fantasy-vii-rebirth");
        game3.setStatus(GameStatus.UPCOMING);
        game3.setReleaseDate(LocalDate.now().plusMonths(6));
        game3.setDeveloper("Square Enix");
        game3.setGenres(new HashSet<>(Set.of(rpgGenre)));
        game3.setPlatforms(new HashSet<>(Set.of(ps5Platform)));

        gameRepository.save(game1);
        gameRepository.save(game2);
        gameRepository.save(game3);
    }

    // ========== GET /api/games (Filtering) Tests ==========

    @Test
    @Transactional
    @WithMockUser
    void getGames_noFilters_returnsAllGames() throws Exception {
        mockMvc.perform(get("/api/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.title == 'Elden Ring')]").exists())
                .andExpect(jsonPath("$[?(@.title == 'Starfield')]").exists())
                .andExpect(jsonPath("$[?(@.title == 'Final Fantasy VII Rebirth')]").exists());
    }

    @Test
    @Transactional
    @WithMockUser
    void getGames_filterBySingleGenre_returnsMatchingGames() throws Exception {
        mockMvc.perform(get("/api/games").param("genres", "Action"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Elden Ring")));
    }

    @Test
    @Transactional
    @WithMockUser
    void getGames_filterByMultiplePlatforms_returnsMatchingGames() throws Exception {
        mockMvc.perform(get("/api/games").param("platforms", "PC,PS5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3))) // All games are on either PC or PS5
                .andExpect(jsonPath("$[?(@.title == 'Elden Ring')]").exists());
    }

    @Test
    @Transactional
    @WithMockUser
    void getGames_filterByStatus_returnsMatchingGames() throws Exception {
        mockMvc.perform(get("/api/games").param("status", "UPCOMING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Final Fantasy VII Rebirth")));
    }

    @Test
    @Transactional
    @WithMockUser
    void getGames_filterByInvalidStatus_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/games").param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @WithMockUser
    void getGames_filterBySearchTerm_returnsMatchingGames() throws Exception {
        mockMvc.perform(get("/api/games").param("search", "field"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Starfield")));
    }

    @Test
    @Transactional
    @WithMockUser
    void getGames_filterByDateAfter_returnsMatchingGames() throws Exception {
        mockMvc.perform(get("/api/games").param("date", "2023-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // Starfield and FFVII
                .andExpect(jsonPath("$[?(@.title == 'Starfield')]").exists())
                .andExpect(jsonPath("$[?(@.title == 'Final Fantasy VII Rebirth')]").exists());
    }

    @Test
    @Transactional
    @WithMockUser
    void getGames_combinedFilters_returnsCorrectlyFilteredGames() throws Exception {
        mockMvc.perform(get("/api/games")
                        .param("genres", "RPG")
                        .param("platforms", "PC")
                        .param("status", "RELEASED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // Elden Ring and Starfield
                .andExpect(jsonPath("$[?(@.title == 'Elden Ring')]").exists())
                .andExpect(jsonPath("$[?(@.title == 'Starfield')]").exists());
    }

    // ========== GET /api/games/{id} Tests ==========

    @Test
    @Transactional
    @WithMockUser
    void viewGame_withExistingId_returnsGameDetails() throws Exception {
        mockMvc.perform(get("/api/games/" + game1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(game1.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Elden Ring")));
    }

    @Test
    @Transactional
    @WithMockUser
    void viewGame_withNonExistentId_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/games/99999"))
                .andExpect(status().isNotFound());
    }

}