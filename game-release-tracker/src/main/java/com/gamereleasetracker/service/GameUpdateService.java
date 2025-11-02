package com.gamereleasetracker.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.gamereleasetracker.dto.api_dto.ApiGameDetailsDto;
import com.gamereleasetracker.dto.api_dto.ApiGameDto;
import com.gamereleasetracker.dto.api_dto.ApiGameResponseDto;
import com.gamereleasetracker.dto.api_dto.ApiGameStoresDto;
import com.gamereleasetracker.dto.api_dto.ApiGameStoresResponseDto;
import com.gamereleasetracker.dto.api_dto.ApiStoreDto;
import com.gamereleasetracker.dto.api_dto.ApiStoresResponseDto;
import com.gamereleasetracker.model.Game;
import com.gamereleasetracker.model.GameStatus;
import com.gamereleasetracker.model.Genre;
import com.gamereleasetracker.model.Platform;
import com.gamereleasetracker.model.PreorderLink;
import com.gamereleasetracker.repository.GameRepository;
import com.gamereleasetracker.repository.GenreRepository;
import com.gamereleasetracker.repository.PlatformRepository;
import com.gamereleasetracker.repository.PreorderLinkRepository;

@Service
public class GameUpdateService{
    private final GameRepository gameRepository;
    private final GenreRepository genreRepository;
    private final PlatformRepository platformRepository;
    private final PreorderLinkRepository preorderLinkRepository;
    private final RestTemplate restTemplate;
    private final NotificationService notificationService;

    // Number of games returned by each query
    private static int page_size = 40;

    // These will be set in constructor from application.properties
    private String apiUrl;
    private String apiKey;
    private List<String> matureTags;

    public GameUpdateService(GameRepository gameRepository, 
                             GenreRepository genreRepository,
                             PlatformRepository platformRepository, 
                             PreorderLinkRepository preorderLinkRepository,
                             RestTemplate restTemplate,
                             NotificationService notificationService,
                             @Value("${rawg.api.url}") String apiUrl,
                             @Value("${rawg.api.key}") String apiKey,
                             @Value("${rawg.api.mature-tags}") List<String> matureTags) {
        this.gameRepository = gameRepository;
        this.genreRepository = genreRepository;
        this.platformRepository = platformRepository;
        this.preorderLinkRepository = preorderLinkRepository;
        this.restTemplate = restTemplate;
        this.notificationService = notificationService;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.matureTags = matureTags;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        UpdateGames();
    }

    @Transactional
    public void UpdateGames() {
        try {
            // Get last month's date so we can search only upcoming games
            String lastMonth = LocalDate.now().minusDays(3).toString();

            // Get map of store id -> store name
            Map<Integer, String> idToStoreNamesMap = GetIdToStoreNamesMap();

            boolean lastPage = false;
            int page = 1;
            // Keep track of all new release game IDs for notification batch update
            List<Long> newlyReleasedGameIds = new ArrayList<>();

            while (!lastPage) {
                // Create API call URL
                String uri = UriComponentsBuilder.fromUriString(apiUrl)
                    .path("/games")
                    .queryParam("key", apiKey)
                    .queryParam("dates", lastMonth + ",2099-12-31") // Search from last month onwards
                    .queryParam("page", page++)
                    .queryParam("page_size", page_size)
                    .queryParam("ordering", "released") // Sort by release date
                    .toUriString();

                // Send GET request and store results as a ApiGameResponseDto
                ResponseEntity<ApiGameResponseDto> response = restTemplate.getForEntity(uri, ApiGameResponseDto.class);

                // Check if the GET request was successful
                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    System.err.println("API returned non-OK status: " + response.getStatusCode());
                    return;
                }

                // Set next URL to next page
                lastPage = response.getBody().getNext() == null;

                // Iterate over each game returned to add/update them in database
                for (ApiGameDto dto : response.getBody().getResults()) {
                    // Ensure release date is not null
                    if (dto.getReleased() == null) {
                        System.err.println("Game " + dto.getName() + " had null release date");
                        continue;
                    }

                    // Check if game already exists in database
                    List<Game> matching = gameRepository.findByRawgGameSlug(dto.getSlug());
                    Game game;
                    boolean isNew = false;
                    GameStatus previousStatus = null;
                    if (matching.size() > 0) {
                        // Game already exists
                        game = matching.get(0);
                        previousStatus = game.getStatus();

                        // Skip game if it has not been updated since it was last updated in the database
                        // The DTO and the entity both use LocalDateTime, so they can be compared directly.
                        LocalDateTime dtoUpdatedTime = dto.getUpdated();
                        if (game.getUpdatedAt() != null && game.getUpdatedAt().isAfter(dtoUpdatedTime)) {
                            continue;
                        }
                        System.out.println("Updating " + dto.getName());
                    }
                    else {
                        // Game does not exist
                        game = new Game();
                        game.setRawgGameSlug(dto.getSlug());
                        isNew = true;
                    }
                    game.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
                    // Set/Update fields
                    game.setTitle(dto.getName());
                    game.setReleaseDate(dto.getReleased());
                    game.setCoverImageUrl(dto.getBackgroundImage());
                    game.setAgeRating(dto.getEsrbRating());
                    GameStatus newStatus = dto.getReleased().isAfter(LocalDate.now()) ? GameStatus.UPCOMING : GameStatus.RELEASED;
                    game.setStatus(newStatus);
                    // Find existing genres or create new ones
                    if (dto.getGenres() != null) {
                        Set<Genre> genres = dto.getGenres().stream()
                            .map(g -> genreRepository.findByName(g)
                                .orElseGet(() -> {
                                    Genre newGenre = new Genre();
                                    newGenre.setName(g);
                                    return genreRepository.save(newGenre);
                                }))
                            .collect(Collectors.toSet());
                        game.setGenres(genres);
                    }
                    // Find existing platforms or create new ones
                    if (dto.getPlatforms() != null) {
                        Set<Platform> platforms = dto.getPlatforms().stream()
                            .map(p -> platformRepository.findByName(p)
                                .orElseGet(() -> {
                                    Platform newPlatform = new Platform();
                                    newPlatform.setName(p);
                                    return platformRepository.save(newPlatform);
                                }))
                            .collect(Collectors.toSet());
                        game.setPlatforms(platforms);
                    }

                    // Get description, developer, and publisher through details API call
                    String detailsUri = UriComponentsBuilder.fromUriString(apiUrl)
                        .path("/games")
                        .path("/")
                        .path(dto.getSlug()) // Add slug to show which game to get details of
                        .queryParam("key", apiKey)
                        .toUriString();
                    
                    ResponseEntity<ApiGameDetailsDto> detailsResponse = restTemplate.getForEntity(detailsUri, ApiGameDetailsDto.class);
                    ApiGameDetailsDto detailsDto = detailsResponse.getBody();
                    game.setDescription(detailsDto.getDescription());
                    game.setPublisher(detailsDto.getPublisher());
                    game.setDeveloper(detailsDto.getDeveloper());

                    // Set mature bool
                    boolean mature = containsMatureTag(dto.getTags());
                    game.setMature(mature);

                    // Save to game repository
                    Game savedGame = gameRepository.save(game);

                    // Get store links through API calls
                    CreatePreorderLinks(game, idToStoreNamesMap);

                    // Check if status changed from UPCOMING to RELEASED
                    if (!isNew && previousStatus == GameStatus.UPCOMING && newStatus == GameStatus.RELEASED) {
                        // Game was just released, add its ID to the list for batch notification
                        newlyReleasedGameIds.add(savedGame.getId());
                    }

                }
            }
            
            // After processing all pages, send notifications for all newly released games at once
            if (!newlyReleasedGameIds.isEmpty()) {
                notificationService.notifyUsersOfGameReleases(newlyReleasedGameIds);
            }
        }
        catch (HttpClientErrorException e) { // 4xx response codes
            System.err.println("Client error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        }
        catch (HttpServerErrorException e) { // 5xx response codes
            System.err.println("Server error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        }
        catch (RestClientException e) { // Errors with parsing the API response
            e.printStackTrace();
            System.err.println("Error calling API: " + e.getMessage());
        }
    }

    private Map<Integer, String> GetIdToStoreNamesMap() {
        Map<Integer, String> idToStoreNamesMap = new HashMap<Integer, String>();

        String storeUri = UriComponentsBuilder.fromUriString(apiUrl)
            .path("/stores")
            .queryParam("key", apiKey)
            .toUriString();

        ResponseEntity<ApiStoresResponseDto> storeLinksResponse = restTemplate.getForEntity(storeUri, ApiStoresResponseDto.class);
        ApiStoresResponseDto storesDto = storeLinksResponse.getBody();

        for (ApiStoreDto storeDto: storesDto.getResults())
            idToStoreNamesMap.put(storeDto.getId(), storeDto.getName());

        return idToStoreNamesMap;
    }

    private void CreatePreorderLinks(Game game, Map<Integer, String> idToStoreNameMap) {
        // Get store links through API
        String storeLinkUri = UriComponentsBuilder.fromUriString(apiUrl)
            .path("/games")
            .path("/")
            .path(game.getRawgGameSlug()) // Add slug to show which game to get details of
            .path("/stores")
            .queryParam("key", apiKey)
            .toUriString();

        ResponseEntity<ApiGameStoresResponseDto> storeLinksResponse = restTemplate.getForEntity(storeLinkUri, ApiGameStoresResponseDto.class);
        ApiGameStoresResponseDto storeLinksDto = storeLinksResponse.getBody();

        // Get existing preorder links for game so we don't add duplicated
        List<PreorderLink> existingLinks = preorderLinkRepository.findByGameId(game.getId());
        Set<String> existingUrls = existingLinks.stream()
            .map(PreorderLink::getUrl)
            .collect(Collectors.toSet());

        for (ApiGameStoresDto storeLinkDto : storeLinksDto.getResults()) {
            // Skip already saved links
            if (existingUrls.contains(storeLinkDto.getUrl())) {
                continue;
            }

            PreorderLink link = new PreorderLink();
            link.setUrl(storeLinkDto.getUrl());
            link.setStoreName(idToStoreNameMap.get(storeLinkDto.getStoreId()));
            link.setGame(game);
            preorderLinkRepository.save(link);
        }
    }

    @Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
    public void scheduledUpdate() {
        UpdateGames();
    }

    private boolean containsMatureTag(Set<String> tags) {
        if (tags == null) return false;
        return matureTags.stream().anyMatch(tags::contains);
    }
}