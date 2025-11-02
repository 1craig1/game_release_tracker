package com.gamereleasetracker;

import com.gamereleasetracker.dto.*;
import com.gamereleasetracker.exception.NotFoundException;
import com.gamereleasetracker.model.*;
import com.gamereleasetracker.repository.GameRepository;
import com.gamereleasetracker.repository.GenreRepository;
import com.gamereleasetracker.repository.PlatformRepository;
import com.gamereleasetracker.repository.PreorderLinkRepository;
import com.gamereleasetracker.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class GameServiceUnitTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private PlatformRepository platformRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private PreorderLinkRepository preorderLinkRepository;

    @InjectMocks
    private GameService gameService;

    private Game game;
    private GameRequestDto gameRequestDto;

    @BeforeEach
    void setUp() {
        game = new Game();
        game.setId(1L);
        game.setTitle("Test Game");
        game.setRawgGameSlug("test-game");
        game.setReleaseDate(LocalDate.now().plusDays(10));
        game.setStatus(GameStatus.UPCOMING);

        Genre genre = new Genre();
        genre.setId(1);
        genre.setName("RPG");

        Platform platform = new Platform();
        platform.setId(1);
        platform.setName("PC");

        game.setGenres(new HashSet<>(Set.of(genre)));
        game.setPlatforms(new HashSet<>(Set.of(platform)));
        game.setPreorderLinks(new HashSet<>());
        gameRequestDto = new GameRequestDto(
                (long) 1,
                "New Test Game",
                "A great new game",
                "http://example.com/cover.jpg",
                LocalDate.now().plusMonths(1),
                "New Publisher",
                "New Developer",
                GameStatus.UPCOMING,
                "E",
                false,
                Set.of(new GenreDto(1, "RPG")),
                Set.of(new PlatformDto(1, "PC")),
                Set.of(new PreorderLinkDto(null, 1L, "Steam", "http://store.steampowered.com"))
        );
    }

    @Test
    void testGetGameById_GameExists_ShouldReturnGameDetailDto() {
        // --- Setup ---
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        // --- Action ---
        Optional<GameDetailDto> result = gameService.getGameById(1L);

        // --- Assertion ---
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(1L);
        assertThat(result.get().title()).isEqualTo("Test Game");
    }

    @Test
    void testGetGameById_GameDoesNotExist_ShouldReturnEmpty() {
        // --- Setup ---
        when(gameRepository.findById(anyLong())).thenReturn(Optional.empty());

        // --- Action ---
        Optional<GameDetailDto> result = gameService.getGameById(99L);

        // --- Assertion ---
        assertThat(result).isNotPresent();
    }

    @Test
    void testAddGame_Success() {
        // --- Setup ---
        // Mock repository calls inside the service method
        when(genreRepository.findByName("RPG")).thenReturn(Optional.of(new Genre()));
        when(platformRepository.findByName("PC")).thenReturn(Optional.of(new Platform()));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game savedGame = invocation.getArgument(0);
            savedGame.setId(2L); // Simulate the DB assigning an ID
            return savedGame;
        });

        // --- Action ---
        GameDetailDto result = gameService.addGame(gameRequestDto);

        // --- Assertion ---
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("New Test Game");

        ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository).save(gameCaptor.capture());
        Game capturedGame = gameCaptor.getValue();

        assertThat(capturedGame.getDeveloper()).isEqualTo("New Developer");
        assertThat(capturedGame.getPreorderLinks()).hasSize(1);
        assertThat(capturedGame.getPreorderLinks().iterator().next().getStoreName()).isEqualTo("Steam");
    }

    @Test
    void testAddGame_CreatesNewGenreAndPlatform_WhenTheyDoNotExist() {
        // --- Setup ---
        GameRequestDto dtoWithNewEntities = new GameRequestDto(
                null, "New Game", "Desc", null, LocalDate.now().plusDays(1), "Dev", "Pub", GameStatus.UPCOMING, "E", false,
                Set.of(new GenreDto(null, "Action")), // "Action" is a new genre
                Set.of(new PlatformDto(null, "Xbox")), // "Xbox" is a new platform
                Collections.emptySet()
        );

        // Mock the "not found" case for the new entities
        when(genreRepository.findByName("Action")).thenReturn(Optional.empty());
        when(platformRepository.findByName("Xbox")).thenReturn(Optional.empty());

        // Mock the save operation that will be triggered by orElseGet()
        when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));
        when(platformRepository.save(any(Platform.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> {
            Game savedGame = inv.getArgument(0);
            savedGame.setId(10L);
            return savedGame;
        });

        // --- Action ---
        gameService.addGame(dtoWithNewEntities);

        // --- Assertion ---
        // Verify that save was called on the repositories for the new entities
        verify(genreRepository, times(1)).save(argThat(g -> "Action".equals(g.getName())));
        verify(platformRepository, times(1)).save(argThat(p -> "Xbox".equals(p.getName())));
    }

    @Test
    void testUpdateGame_Success() {
        // --- Setup ---
        // The DTO for an update needs an ID to find the game.
        GameRequestDto updateDto = new GameRequestDto(
                1L,
                "New Test Game",
                "A great new game",
                "http://example.com/cover.jpg",
                LocalDate.now().plusMonths(1),
                "New Publisher",
                "New Developer",
                GameStatus.UPCOMING,
                "E",
                false,
                Set.of(new GenreDto(1, "RPG")),
                Set.of(new PlatformDto(1, "PC")),
                Set.of(new PreorderLinkDto(null, 1L, "Steam", "http://store.steampowered.com")));

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(genreRepository.findByName(anyString())).thenReturn(Optional.of(new Genre()));
        when(platformRepository.findByName(anyString())).thenReturn(Optional.of(new Platform()));
        when(gameRepository.save(any(Game.class))).thenReturn(game);

        // --- Action ---
        gameService.updateGame(updateDto);

        // --- Assertion ---
        ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository).save(gameCaptor.capture());
        Game updatedGame = gameCaptor.getValue();

        assertThat(updatedGame.getTitle()).isEqualTo("New Test Game");
        assertThat(updatedGame.getDeveloper()).isEqualTo("New Developer");
        assertThat(updatedGame.getPreorderLinks()).hasSize(1);
    }

    @Test
    void testUpdateGame_CreatesNewGenreAndPlatform_WhenTheyDoNotExist() {
        // --- Setup ---
        // DTO with a new genre ("Action") and a new platform ("Xbox") that don't exist yet
        GameRequestDto updateDtoWithNewEntities = new GameRequestDto(
                1L,
                "Updated Title",
                "Updated Description",
                null, LocalDate.now().plusDays(1), null, "Dev", GameStatus.UPCOMING, null, false,
                Set.of(new GenreDto(null, "Action")),
                Set.of(new PlatformDto(null, "Xbox")),
                Collections.emptySet()
        );

        Genre newGenre = new Genre();
        newGenre.setId(2);
        newGenre.setName("Action");

        Platform newPlatform = new Platform();
        newPlatform.setId(2);
        newPlatform.setName("Xbox");

        // Mock the initial state: game exists, but the new genre/platform do not
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(genreRepository.findByName("Action")).thenReturn(Optional.empty());
        when(platformRepository.findByName("Xbox")).thenReturn(Optional.empty());

        // Mock the save operations that will be triggered by orElseGet()
        when(genreRepository.save(any(Genre.class))).thenReturn(newGenre);
        when(platformRepository.save(any(Platform.class))).thenReturn(newPlatform);
        
        // Mock the final save call on the game repository
        when(gameRepository.save(any(Game.class))).thenReturn(game);

        // --- Action ---
        gameService.updateGame(updateDtoWithNewEntities);

        // --- Assertion ---
        // Verify that save was called on the repositories for the new entities
        verify(genreRepository, times(1)).save(argThat(g -> "Action".equals(g.getName())));
        verify(platformRepository, times(1)).save(argThat(p -> "Xbox".equals(p.getName())));
    }

    @Test
    void testUpdateGame_OnlyUpdatesNonNullFields() {
        // --- Setup ---
        // Create a DTO where most fields are null, only ageRating and coverImageUrl are provided.
        GameRequestDto partialUpdateDto = new GameRequestDto(
                1L,
                null, // title
                null, // description
                "http://new.cover.url/image.jpg", // coverImageUrl
                null, // releaseDate
                null, // publisher
                null, // developer
                null, // status
                "M for Mature", // ageRating
                true,
                null, // genres
                null, // platforms
                null  // preorderLinks
        );

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenReturn(game);

        // --- Action ---
        gameService.updateGame(partialUpdateDto);

        // --- Assertion ---
        ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository).save(gameCaptor.capture());
        Game capturedGame = gameCaptor.getValue();

        assertThat(capturedGame.getTitle()).isEqualTo("Test Game"); // Should remain unchanged
        assertThat(capturedGame.getAgeRating()).isEqualTo("M for Mature"); // Should be updated
        assertThat(capturedGame.getCoverImageUrl()).isEqualTo("http://new.cover.url/image.jpg"); // Should be updated
    }

    @Test
    void testUpdateGame_GameNotFound_ShouldThrowException() {
        // --- Setup ---
        // The DTO for an update needs an ID to find the game.
        GameRequestDto updateDto = new GameRequestDto(
                99L,
                "New Test Game",
                "A great new game",
                "http://example.com/cover.jpg",
                LocalDate.now().plusMonths(1),
                "New Publisher",
                "New Developer",
                GameStatus.UPCOMING,
                "E",
                false,
                Set.of(new GenreDto(1, "RPG")),
                Set.of(new PlatformDto(1, "PC")),
                Set.of(new PreorderLinkDto(null, 1L, "Steam", "http://store.steampowered.com")));

        when(gameRepository.findById(anyLong())).thenReturn(Optional.empty());

        // --- Action & Assertion ---
        assertThrows(NotFoundException.class, () -> gameService.updateGame(updateDto));
    }

    @Test
    void testDeleteGame_Success() {
        // --- Setup ---
        when(gameRepository.existsById(1L)).thenReturn(true);
        doNothing().when(gameRepository).deleteById(1L);

        // --- Action ---
        gameService.deleteGame(1L);

        // --- Assertion ---
        verify(gameRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteGame_GameNotFound_ShouldThrowException() {
        // --- Setup ---
        when(gameRepository.existsById(99L)).thenReturn(false);

        // --- Action & Assertion ---
        assertThrows(NotFoundException.class, () -> gameService.deleteGame(99L));
        verify(gameRepository, never()).deleteById(anyLong());
    }

    @Test
    void testGetGamesByStatus_ShouldReturnMatchingGames() {
        // --- Setup ---
        Game upcomingGame = new Game();
        upcomingGame.setId(2L);
        upcomingGame.setTitle("Upcoming Game");
        upcomingGame.setStatus(GameStatus.UPCOMING);

        when(gameRepository.findByStatus(GameStatus.UPCOMING)).thenReturn(List.of(upcomingGame));

        // --- Action ---
        List<GameSummaryDto> result = gameService.getGamesByStatus(GameStatus.UPCOMING);

        // --- Assertion ---
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Upcoming Game");
    }

    @Test
    void testGetGamesByPlatform_ShouldReturnMatchingGames() {
        // --- Setup ---
        Game pcGame = new Game();
        pcGame.setId(3L);
        pcGame.setTitle("PC Game");

        when(gameRepository.findByPlatforms_NameIn(Set.of("PC"))).thenReturn(List.of(pcGame));

        // --- Action ---
        List<GameSummaryDto> result = gameService.getGamesByPlatform(Set.of("PC"));

        // --- Assertion ---
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("PC Game");
    }

    @Test
    void testGetGamesByGenre_ShouldReturnMatchingGames() {
        // --- Setup ---
        Game rpgGame = new Game();
        rpgGame.setId(4L);
        rpgGame.setTitle("RPG Game");

        when(gameRepository.findByGenres_NameIn(Set.of("RPG"))).thenReturn(List.of(rpgGame));

        // --- Action ---
        List<GameSummaryDto> result = gameService.getGamesByGenre(Set.of("RPG"));

        // --- Assertion ---
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("RPG Game");
    }

    @Test
    void testFindGamesByReleaseDateAfter_ShouldReturnMatchingGames() {
        // --- Setup ---
        LocalDate today = LocalDate.now();
        when(gameRepository.findByReleaseDateAfterOrderByReleaseDateAsc(today)).thenReturn(List.of(game));

        // --- Action ---
        List<GameSummaryDto> result = gameService.getGamesAfterDate(today);

        // --- Assertion ---
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Test Game");
    }

    @Test
    void testSearchGames_ShouldReturnMatchingGames() {
        // --- Setup ---
        String searchTerm = "Test";
        Game anotherGame = new Game();
        anotherGame.setId(5L);
        anotherGame.setTitle("Another Test Game");

        when(gameRepository.findByTitleContainingIgnoreCase(searchTerm)).thenReturn(List.of(game, anotherGame));

        // --- Action ---
        List<GameSummaryDto> result = gameService.searchGames(searchTerm);

        // --- Assertion ---
        assertThat(result).hasSize(2);
        assertThat(result).extracting(GameSummaryDto::title).containsExactlyInAnyOrder("Test Game", "Another Test Game");
    }

    @Test
    void testGetAllGames_ShouldReturnAllGamesAsSummaryDtos() {
        // --- Setup ---
        Game anotherGame = new Game();
        anotherGame.setId(2L);
        anotherGame.setTitle("Another Game");
        anotherGame.setReleaseDate(LocalDate.now());
        anotherGame.setStatus(GameStatus.RELEASED);

        when(gameRepository.findAll()).thenReturn(List.of(game, anotherGame));

        // --- Action ---
        List<GameSummaryDto> result = gameService.getAllGames();

        // --- Assertion ---
        assertThat(result).hasSize(2);
        assertThat(result).extracting(GameSummaryDto::title).containsExactlyInAnyOrder("Test Game", "Another Game");
    }

    @Test
    void testGetAllGenres_ShouldReturnAllGenresAsDtos() {
        // --- Setup ---
        Genre genre1 = new Genre();
        genre1.setId(1);
        genre1.setName("RPG");

        Genre genre2 = new Genre();
        genre2.setId(2);
        genre2.setName("Action");

        when(genreRepository.findAll()).thenReturn(List.of(genre1, genre2));

        // --- Action ---
        Set<GenreDto> result = gameService.getAllGenres();

        // --- Assertion ---
        assertThat(result).hasSize(2);
        assertThat(result).extracting(GenreDto::name).containsExactlyInAnyOrder("RPG", "Action");
    }

    @Test
    void testGetAllPlatforms_ShouldReturnAllPlatformsAsDtos() {
        // --- Setup ---
        Platform platform1 = new Platform();
        platform1.setId(1);
        platform1.setName("PC");

        when(platformRepository.findAll()).thenReturn(List.of(platform1));

        // --- Action ---
        Set<PlatformDto> result = gameService.getAllPlatforms();

        // --- Assertion ---
        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().name()).isEqualTo("PC");
    }

}
