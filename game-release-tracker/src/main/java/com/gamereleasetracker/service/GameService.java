package com.gamereleasetracker.service;

import com.gamereleasetracker.model.Game;
import com.gamereleasetracker.model.GameStatus;
import com.gamereleasetracker.model.Genre;
import com.gamereleasetracker.model.Platform;
import com.gamereleasetracker.model.PreorderLink;
import com.gamereleasetracker.dto.GameRequestDto;
import com.gamereleasetracker.dto.GenreDto;
import com.gamereleasetracker.dto.GameSummaryDto;
import com.gamereleasetracker.dto.GameDetailDto;
import com.gamereleasetracker.dto.PlatformDto;
import com.gamereleasetracker.dto.PreorderLinkDto;
import com.gamereleasetracker.repository.*;
import com.gamereleasetracker.exception.NotFoundException;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;

@Service
public class GameService {
    private final GameRepository gameRepository;
    private final PlatformRepository platformRepository;
    private final GenreRepository genreRepository;
    private final PreorderLinkRepository preorderLinkRepository;
    private final GameSpecification specBuilder;
    @Autowired
    public GameService(GameRepository gameRepository, PlatformRepository platformRepository,
                       GenreRepository genreRepository, PreorderLinkRepository preorderLinkRepository,
                       GameSpecification specBuilder) { // Add to constructor
        this.gameRepository = gameRepository;
        this.platformRepository = platformRepository;
        this.genreRepository = genreRepository;
        this.preorderLinkRepository = preorderLinkRepository;
        this.specBuilder = specBuilder; // Assign in constructor
    }

    /**
     * Searches for games by title.
     * This method is typically used to implement search functionality in the application.
     *
     * @param searchTerm The term to search for in game titles.
     * @return A list of game summary dtos that match the search term.
     */
    @Deprecated
    public List<GameSummaryDto> searchGames(String searchTerm) {
        return gameRepository.findByTitleContainingIgnoreCase(searchTerm).stream()
                .map(this::convertToGameSummaryDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Retrieves a list of games by their release status.
     * This method is typically used to filter games by their current status.
     *
     * @param status The status to filter by (e.g., UPCOMING, RELEASED).
     * @return A list of game summary dtos that match the specified status.
     */
    @Deprecated
    public List<GameSummaryDto> getGamesByStatus(GameStatus status) {
        return gameRepository.findByStatus(status).stream()
                .map(this::convertToGameSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of games available on specific platforms.
     * This method is typically used to filter games by platform.
     *
     * @param platformNames The names of the platforms (e.g., "PC", "PlayStation").
     * @return A list of game summary dtos that are available on the specified platforms.
     */
    @Deprecated
    public List<GameSummaryDto> getGamesByPlatform(Set<String> platformNames) {
        return gameRepository.findByPlatforms_NameIn(platformNames).stream()
                .map(this::convertToGameSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of games that are scheduled for release after a given date.
     * This method is typically used to display upcoming games.
     *
     * @param currentDate The date to filter games by (e.g., current date).
     * @return A list of game summary dtos that are scheduled for release after the specified date.
     */
    @Deprecated
    public List<GameSummaryDto> getGamesAfterDate(LocalDate currentDate) {
        return gameRepository.findByReleaseDateAfterOrderByReleaseDateAsc(currentDate).stream()
                .map(this::convertToGameSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of games by their genre.
     * This method is typically used to filter games by genre.
     *
     * @param genreNames The genre to filter by (e.g., "RPG", "Shooter").
     * @return A list of game summary dtos that match the specified genre.
     */
    @Deprecated
    public List<GameSummaryDto> getGamesByGenre(Set<String> genreNames) {
        return gameRepository.findByGenres_NameIn(genreNames).stream()
                .map(this::convertToGameSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a game by its ID.
     * This method is typically used to display game details.
     *
     * @param id The ID of the game to retrieve.
     * @return An Optional containing the game detail dto if found, or empty if not found.
     */
    public Optional<GameDetailDto> getGameById(Long id) {
        return gameRepository.findById(id)
                .map(this::convertToGameDetailDto);
    }

    /**
     * Dynamically filters games based on a combination of criteria.
     * This method uses the JPA Specification API to build the query piece by piece.
     *
     * @param genres    A set of genre names to filter by (optional).
     * @param platforms A set of platform names to filter by (optional).
     * @param status    A release status to filter by (optional).
     * @param search    A search term for the game title (optional).
     * @param afterDate A date to find games released after (optional).
     * @return A sorted list of games that match all provided filters.
     */
    public List<GameSummaryDto> getFilteredGames(Set<String> genres, Set<String> platforms, GameStatus status, String search, LocalDate afterDate) {

        // 1. Start with an empty, non-null Specification that does nothing.
        Specification<Game> spec = Specification.unrestricted();

        // 2. Conditionally chain each filter. If a filter's value is null or empty,
        // its corresponding specification method will return a "do nothing" predicate,
        // so it won't affect the query.
        if (status != null) {
            spec = spec.and(specBuilder.hasStatus(status));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and(specBuilder.titleContains(search));
        }
        if (afterDate != null) {
            spec = spec.and(specBuilder.releasedAfter(afterDate));
        }
        if (genres != null && !genres.isEmpty()) {
            spec = spec.and(specBuilder.hasGenres(genres));
        }
        if (platforms != null && !platforms.isEmpty()) {
            spec = spec.and(specBuilder.hasPlatforms(platforms));
        }

        // Define how the final results should be sorted.
        Sort sort = Sort.by(
                Sort.Order.asc("releaseDate"),
                Sort.Order.asc("title"));

        // 3. Execute the final, combined specification and map the results to DTOs.
        return gameRepository.findAll(spec, sort).stream()
                .map(this::convertToGameSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all games from the repository.
     * This method is typically used to display a list of all games.
     *
     * @return A list of all game summary dtos.
     */
    public List<GameSummaryDto> getAllGames() {
        return gameRepository.findAll().stream()
                .map(this::convertToGameSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Adds a new game to the repository.
     * This method is typically used to add new games to the database.
     * Should only be executable by admins or authorized users.
     *
     * @param gameRequestDto The GameRequestDto containing game information.
     * @return The saved game detail dto.
     */
    @Transactional
    public GameDetailDto addGame(GameRequestDto gameRequestDto) {
        // Create a new Game entity
        Game game = new Game();
        game.setTitle(gameRequestDto.title());
        game.setDescription(gameRequestDto.description());
        game.setReleaseDate(gameRequestDto.releaseDate());
        game.setStatus(gameRequestDto.status());
        // Convert GenreDto to Genre entities
        Set<Genre> genres = gameRequestDto.genres().stream()
                .map(dto -> genreRepository.findByName(dto.name())
                        .orElseGet(() -> {
                            Genre newGenre = new Genre();
                            newGenre.setName(dto.name());
                            return genreRepository.save(newGenre);
                        }))
                .collect(Collectors.toSet());
        game.setGenres(genres);
        // Convert PlatformDto to Platform entities
        Set<Platform> platforms = gameRequestDto.platforms().stream()
                .map(dto -> platformRepository.findByName(dto.name())
                        .orElseGet(() -> {
                            Platform newPlatform = new Platform();
                            newPlatform.setName(dto.name());
                            return platformRepository.save(newPlatform);
                        }))
                .collect(Collectors.toSet());
        game.setPlatforms(platforms);
        game.setCoverImageUrl(gameRequestDto.coverImageUrl());
        game.setDeveloper(gameRequestDto.developer());
        game.setPublisher(gameRequestDto.publisher());
        game.setAgeRating(gameRequestDto.ageRating());
        game.setPreorderLinks(gameRequestDto.preorderLinks().stream()
                .map(dto -> {
                        PreorderLink link = new PreorderLink();
                        link.setGame(game);
                        link.setStoreName(dto.storeName());
                        link.setUrl(dto.url());
                        return link;
                })
                .collect(Collectors.toSet()));

        // Save the new game to the repository
        return convertToGameDetailDto(gameRepository.save(game));
    }

    /**
     * Updates an existing game.
     * This method is typically used to modify the details of a game.
     * Should only be executable by admins or authorized users.
     *
     * @param gameRequestDto The GameRequestDto containing updated game information.
     * @return The updated game detail Dto.
     */
    @Transactional
    public GameDetailDto updateGame(GameRequestDto gameRequestDto) {
        Game existingGame = gameRepository.findById(gameRequestDto.id())
                .orElseThrow(() -> new NotFoundException("Game not found with id: " + gameRequestDto.id()));

        // Update fields if they are provided in the request DTO
        if (gameRequestDto.title() != null) {
            existingGame.setTitle(gameRequestDto.title());
        }
        if (gameRequestDto.description() != null) {
            existingGame.setDescription(gameRequestDto.description());
        }
        if (gameRequestDto.releaseDate() != null) {
            existingGame.setReleaseDate(gameRequestDto.releaseDate());
        }
        if (gameRequestDto.status() != null) {
            existingGame.setStatus(gameRequestDto.status());
        }
        if (gameRequestDto.genres() != null) {
            existingGame.getGenres().clear();
            Set<Genre> genres = gameRequestDto.genres().stream()
                    .map(dto -> genreRepository.findByName(dto.name())
                            .orElseGet(() -> {
                                Genre newGenre = new Genre();
                                newGenre.setName(dto.name());
                                return genreRepository.save(newGenre);
                            }))
                    .collect(Collectors.toSet());
            existingGame.getGenres().addAll(genres);
        }
        if (gameRequestDto.platforms() != null) {
            existingGame.getPlatforms().clear();
            Set<Platform> platforms = gameRequestDto.platforms().stream()
                    .map(dto -> platformRepository.findByName(dto.name())
                            .orElseGet(() -> {
                                Platform newPlatform = new Platform();
                                newPlatform.setName(dto.name());
                                return platformRepository.save(newPlatform);
                            }))
                    .collect(Collectors.toSet());
            existingGame.getPlatforms().addAll(platforms);
        }
        if (gameRequestDto.coverImageUrl() != null) {
            existingGame.setCoverImageUrl(gameRequestDto.coverImageUrl());
        }
        if (gameRequestDto.developer() != null) {
            existingGame.setDeveloper(gameRequestDto.developer());
        }
        if (gameRequestDto.publisher() != null) {
            existingGame.setPublisher(gameRequestDto.publisher());
        }
        if (gameRequestDto.ageRating() != null) {
            existingGame.setAgeRating(gameRequestDto.ageRating());
        }
        if (gameRequestDto.preorderLinks() != null) {
            existingGame.getPreorderLinks().clear();
            Set<PreorderLink> preorderLinks = gameRequestDto.preorderLinks().stream()
                    .map(dto -> {
                            PreorderLink link = new PreorderLink();
                            link.setGame(existingGame);
                            link.setStoreName(dto.storeName());
                            link.setUrl(dto.url());
                            return link;
                    })
                    .collect(Collectors.toSet());
            existingGame.getPreorderLinks().addAll(preorderLinks);
        }
        return convertToGameDetailDto(gameRepository.save(existingGame));
    }

    /**
     * Deletes a game by its ID.
     * This method should be used with caution as it will remove the game from the database.
     * Should be executable only by admins or authorized users
     *
     * @param id The ID of the game to delete.
     */
    @Transactional
    public void deleteGame(Long id) {
        if (!gameRepository.existsById(id)) {
            throw new NotFoundException("Game with ID " + id + " not found");
        }
        gameRepository.deleteById(id);
    }

    /**
     * Retrieves the GameRepository instance.
     * This method is typically used for testing purposes.
     *
     * @return The GameRepository instance.
     */
    @Deprecated
    public GameRepository getGameRepository() {
        return gameRepository;
    }

    /**
     * Retrieves all platforms from the repository.
     * This method is typically used to display all platforms.
     *
     * @return A set of all platform dtos.
     */
    public Set<PlatformDto> getAllPlatforms() {
        return platformRepository.findAll().stream()
            .map(this::convertToPlatformDto)
            .collect(Collectors.toSet());
    }

    /**
     * Retrieves all genres from the repository.
     * This method is typically used to display all genres.
     *
     * @return A set of all genre dtos.
     */
    public Set<GenreDto> getAllGenres() {
        return genreRepository.findAll().stream()
            .map(this::convertToGenreDto)    
            .collect(Collectors.toSet());
    }

    public GameDetailDto convertToGameDetailDto(Game game) {
        Set<GenreDto> genreDtos = game.getGenres().stream()
                .map(this::convertToGenreDto)
                .collect(Collectors.toSet());

        Set<PlatformDto> platformDtos = game.getPlatforms().stream()
                .map(this::convertToPlatformDto)
                .collect(Collectors.toSet());

        Set<PreorderLinkDto> preorderLinkDtos = game.getPreorderLinks().stream()
                .map(this::convertToPreorderLinkDto)
                .collect(Collectors.toSet());

        return new GameDetailDto(
                game.getId(),
                game.getTitle(),
                game.getDescription(),
                game.getCoverImageUrl(),
                game.getReleaseDate(),
                game.getDeveloper(),
                game.getPublisher(),
                game.getRawgGameSlug(),
                game.getStatus(),
                game.getAgeRating(),
                game.isMature(),
                genreDtos,
                platformDtos,
                preorderLinkDtos
        );
    }

    public GameSummaryDto convertToGameSummaryDto(Game game) {
        Set<GenreDto> genreDtos = game.getGenres().stream()
                .map(this::convertToGenreDto)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        Set<PlatformDto> platformDtos = game.getPlatforms().stream()
                .map(this::convertToPlatformDto)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        return new GameSummaryDto(
                game.getId(),
                game.getTitle(),
                game.getDescription(),
                game.getCoverImageUrl(),
                game.getReleaseDate(),
                game.getStatus(),
                genreDtos,
                platformDtos,
                game.isMature()
        );
    }

    public GenreDto convertToGenreDto(Genre genre) {
        return new GenreDto(
            genre.getId(),
            genre.getName()
        );
    }

    public PlatformDto convertToPlatformDto(Platform platform) {
        return new PlatformDto(
            platform.getId(),
            platform.getName()
        );
    }

    public PreorderLinkDto convertToPreorderLinkDto(PreorderLink link) {
        return new PreorderLinkDto(
            link.getId(),
            link.getGame().getId(),
            link.getStoreName(),
            link.getUrl()
        );
    }
}
