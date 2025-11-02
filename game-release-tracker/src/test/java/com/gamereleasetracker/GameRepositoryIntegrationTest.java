package com.gamereleasetracker;

import com.gamereleasetracker.model.Game;
import com.gamereleasetracker.model.GameStatus;
import com.gamereleasetracker.model.Platform;
import com.gamereleasetracker.model.Genre;
import com.gamereleasetracker.repository.GameRepository;
import com.gamereleasetracker.repository.PlatformRepository;
import com.gamereleasetracker.repository.GenreRepository;
import com.gamereleasetracker.service.GameUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// @SpringBootTest
// @ActiveProfiles("test") // Ensures this test runs with the 'test' configuration
@Transactional
class GameRepositoryIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private GameUpdateService gameUpdateService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PlatformRepository platformRepository;

    // --- ADDED ---
    @Autowired
    private GenreRepository genreRepository;

    private Platform pcPlatform;
    private Platform ps5Platform;
    // --- ADDED ---
    private Genre rpgGenre;
    private Genre shooterGenre;

    // This setup runs before each test to create and save platform and genre data
    @BeforeEach
    @Transactional
    public void setUp() {
        pcPlatform = new Platform();
        pcPlatform.setName("PC");
        platformRepository.save(pcPlatform);

        ps5Platform = new Platform();
        ps5Platform.setName("PS5");
        platformRepository.save(ps5Platform);

        // --- ADDED ---
        // Setup for genres
        rpgGenre = new Genre();
        rpgGenre.setName("RPG");
        genreRepository.save(rpgGenre);

        shooterGenre = new Genre();
        shooterGenre.setName("Shooter");
        genreRepository.save(shooterGenre);
    }

    /**
     * Tests the ability to save a new Game entity and confirm its properties,
     * checking if the title, developer and platform are correctly saved and retrieved.
     */
    @Test
    @Transactional
    void testSaveAndFindGame() {
        // --- Setup ---
        Game newGame = new Game();
        newGame.setTitle("Shartfield");
        newGame.setDeveloper("Hodd Toward");
        newGame.setReleaseDate(LocalDate.now());
        newGame.setRawgGameSlug("shartfield"); // --- ADDED ---
        newGame.getPlatforms().add(pcPlatform);
        newGame.setStatus(GameStatus.UPCOMING);

        // --- Action ---
        Game savedGame = gameRepository.save(newGame);

        // --- Assertion ---
        Game foundGame = gameRepository.findById(savedGame.getId()).orElseThrow();
        assertThat(foundGame.getTitle()).isEqualTo("Shartfield");
        assertThat(foundGame.getDeveloper()).isEqualTo("Hodd Toward");
        assertThat(foundGame.getRawgGameSlug()).isEqualTo("shartfield"); // --- ADDED ---
        assertThat(foundGame.getPlatforms()).contains(pcPlatform);
    }

    /**
     * Tests the custom query method `findByTitleContainingIgnoreCase` to ensure
     * it correctly finds games whose titles contain a specific substring,
     * regardless of case.
     */
    @Test
    @Transactional
    void testFindGamesByTitleContainingIgnoreCase() {
        // --- Setup ---
        Game game1 = new Game();
        game1.setTitle("Cyberpunk 2077");
        game1.setReleaseDate(LocalDate.now());
        game1.setRawgGameSlug("cyberpunk-2077"); // --- ADDED ---
        game1.getPlatforms().add(pcPlatform);
        game1.setStatus(GameStatus.RELEASED);

        Game game2 = new Game();
        game2.setTitle("The Witcher 3: Wild Hunt");
        game2.setReleaseDate(LocalDate.now());
        game2.setRawgGameSlug("the-witcher-3-wild-hunt"); // --- ADDED ---
        game2.getPlatforms().add(pcPlatform);
        game2.setStatus(GameStatus.RELEASED);

        Game game3 = new Game();
        game3.setTitle("CYBERPUNK EDGERUNNERS UPDATE");
        game3.setReleaseDate(LocalDate.now());
        game3.setRawgGameSlug("cyberpunk-edgerunners-update"); // --- ADDED ---
        game3.getPlatforms().add(ps5Platform);
        game3.setStatus(GameStatus.RELEASED);

        gameRepository.saveAll(Arrays.asList(game1, game2, game3));
        // --- Action ---
        List<Game> foundGames = gameRepository.findByTitleContainingIgnoreCase("cyberpunk");

        // --- Assertion ---
        assertThat(foundGames).hasSize(2);
    }

    /**
     * Tests the custom query method `findByReleaseDateAfterOrderByReleaseDateAsc` to
     * find all games with a release date in the future and then verifies that the
     * results are correctly ordered by their release date in ascending order.
     */
    @Test
    @Transactional
    void testFindUpcomingGamesAfterGivenDateAndOrderThemByReleaseDate() {
        // --- Setup ---
        LocalDate now = LocalDate.now();

        Game pastGame = new Game();
        pastGame.setTitle("Past Game");
        pastGame.setReleaseDate(now.minusDays(10));
        pastGame.setRawgGameSlug("past-game"); // --- ADDED ---
        pastGame.getPlatforms().add(pcPlatform);
        pastGame.setStatus(GameStatus.RELEASED);

        Game futureGame1 = new Game();
        futureGame1.setTitle("Future Game 1 (Releases Soon)");
        futureGame1.setReleaseDate(now.plusDays(10));
        futureGame1.setRawgGameSlug("future-game-1"); // --- ADDED ---
        futureGame1.getPlatforms().add(pcPlatform);
        futureGame1.setStatus(GameStatus.UPCOMING);

        Game futureGame2 = new Game();
        futureGame2.setTitle("Future Game 2 (Releases Later)");
        futureGame2.setReleaseDate(now.plusDays(30));
        futureGame2.setRawgGameSlug("future-game-2"); // --- ADDED ---
        futureGame2.getPlatforms().add(ps5Platform);
        futureGame2.setStatus(GameStatus.UPCOMING);

        gameRepository.saveAll(Arrays.asList(pastGame, futureGame1, futureGame2));

        // --- Action ---
        List<Game> upcomingGames = gameRepository.findByReleaseDateAfterOrderByReleaseDateAsc(now);

        // --- Assertion ---
        assertThat(upcomingGames).hasSize(2);
        // Verify the ascending order
        assertThat(upcomingGames.get(0).getTitle()).isEqualTo("Future Game 1 (Releases Soon)");
        assertThat(upcomingGames.get(1).getTitle()).isEqualTo("Future Game 2 (Releases Later)");
    }

    /**
     * Tests the `findByStatus` method to ensure it correctly retrieves
     * a list of games that match the specified `GameStatus` enum value.
     */
    @Test
    @Transactional
    void testFindGamesByStatus() {
        // --- Setup ---
        Game releasedGame = new Game();
        releasedGame.setTitle("Released Game");
        releasedGame.setReleaseDate(LocalDate.now());
        releasedGame.setRawgGameSlug("released-game"); // --- ADDED ---
        releasedGame.getPlatforms().add(pcPlatform);
        releasedGame.setStatus(GameStatus.RELEASED);

        Game upcomingGame = new Game();
        upcomingGame.setTitle("Upcoming Game");
        upcomingGame.setReleaseDate(LocalDate.now());
        upcomingGame.setRawgGameSlug("upcoming-game"); // --- ADDED ---
        upcomingGame.getPlatforms().add(ps5Platform);
        upcomingGame.setStatus(GameStatus.UPCOMING);

        gameRepository.saveAll(Arrays.asList(releasedGame, upcomingGame));

        // --- Action ---
        List<Game> foundGames = gameRepository.findByStatus(GameStatus.RELEASED);

        // --- Assertion ---
        assertThat(foundGames).hasSize(1);
        assertThat(foundGames.get(0).getTitle()).isEqualTo("Released Game");
    }

    /**
     * Tests the `findByPlatforms_Name` method to ensure it correctly filters
     * games by their associated platform's name.
     */
    @Test
    @Transactional
    void testFindGamesByPlatformName() {
        // --- Setup ---
        Game pcOnlyGame = new Game();
        pcOnlyGame.setTitle("PC Only Game");
        pcOnlyGame.setReleaseDate(LocalDate.now());
        pcOnlyGame.setRawgGameSlug("pc-only-game"); // --- ADDED ---
        pcOnlyGame.setStatus(GameStatus.RELEASED);
        pcOnlyGame.getPlatforms().add(pcPlatform);

        Game ps5OnlyGame = new Game();
        ps5OnlyGame.setTitle("PS5 Only Game");
        ps5OnlyGame.setReleaseDate(LocalDate.now());
        ps5OnlyGame.setRawgGameSlug("ps5-only-game"); // --- ADDED ---
        ps5OnlyGame.setStatus(GameStatus.UPCOMING);
        ps5OnlyGame.getPlatforms().add(ps5Platform);

        Game multiPlatformGame = new Game();
        multiPlatformGame.setTitle("Multi-Platform Game");
        multiPlatformGame.setReleaseDate(LocalDate.now());
        multiPlatformGame.setRawgGameSlug("multi-platform-game"); // --- ADDED ---
        multiPlatformGame.setStatus(GameStatus.UPCOMING);
        multiPlatformGame.getPlatforms().add(pcPlatform);
        multiPlatformGame.getPlatforms().add(ps5Platform);

        gameRepository.saveAll(Arrays.asList(pcOnlyGame, ps5OnlyGame, multiPlatformGame));

        // --- Action ---
        List<Game> foundGames = gameRepository.findByPlatforms_NameIn(Set.of("PC"));

        // --- Assertion ---
        assertThat(foundGames).hasSize(2);
        assertThat(foundGames).extracting(Game::getTitle)
                .containsExactlyInAnyOrder("PC Only Game", "Multi-Platform Game");
    }

    /**
     * --- UPDATED ---
     * Tests the `findByGenres_Name` method to ensure it correctly retrieves games
     * that match the specified genre's name via the many-to-many relationship.
     */
    @Test
    @Transactional
    void testFindGamesByGenreName() { // --- RENAMED ---
        // --- Setup ---
        Game rpgGame1 = new Game();
        rpgGame1.setTitle("RPG Game 1");
        rpgGame1.setReleaseDate(LocalDate.now());
        rpgGame1.setRawgGameSlug("rpg-game-1"); // --- ADDED ---
        rpgGame1.getPlatforms().add(pcPlatform);
        rpgGame1.setStatus(GameStatus.RELEASED);
        rpgGame1.getGenres().add(rpgGenre); // --- UPDATED ---

        Game shooterGame = new Game();
        shooterGame.setTitle("Shooter Game");
        shooterGame.setReleaseDate(LocalDate.now());
        shooterGame.setRawgGameSlug("shooter-game"); // --- ADDED ---
        shooterGame.getPlatforms().add(ps5Platform);
        shooterGame.setStatus(GameStatus.UPCOMING);
        shooterGame.getGenres().add(shooterGenre); // --- UPDATED ---

        gameRepository.saveAll(Arrays.asList(rpgGame1, shooterGame));

        // --- Action ---
        List<Game> foundGames = gameRepository.findByGenres_Name("RPG"); // --- UPDATED ---

        // --- Assertion ---
        assertThat(foundGames).hasSize(1);
        assertThat(foundGames.get(0).getTitle()).isEqualTo("RPG Game 1");
    }
    
}