package com.gamereleasetracker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.gamereleasetracker.dto.api_dto.ApiGameDetailsDto;
import com.gamereleasetracker.dto.api_dto.ApiGameDto;
import com.gamereleasetracker.dto.api_dto.ApiGameResponseDto;
import com.gamereleasetracker.dto.api_dto.ApiGameStoresResponseDto;
import com.gamereleasetracker.dto.api_dto.ApiStoresResponseDto;
import com.gamereleasetracker.model.Game;
import com.gamereleasetracker.repository.GameRepository;
import com.gamereleasetracker.repository.GenreRepository;
import com.gamereleasetracker.repository.PlatformRepository;
import com.gamereleasetracker.repository.PreorderLinkRepository;
import com.gamereleasetracker.service.GameUpdateService;
import com.gamereleasetracker.service.NotificationService;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class GameUpdateServiceUnitTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private PlatformRepository platformRepository;

    @Mock
    private PreorderLinkRepository preorderLinkRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private NotificationService notificationService;

    private GameUpdateService service;

    @BeforeEach
    void setup() {
        service = new GameUpdateService(
            gameRepository, 
            genreRepository, 
            platformRepository, 
            preorderLinkRepository,
            restTemplate, 
            notificationService, 
            "https://fake-api", 
            "fakekey", 
            Collections.singletonList("nsfw")
        );
    }

    @Test
    void createNewGame() {
        // Create fake API game DTO
        ApiGameDto apiGameDto = new ApiGameDto();
        apiGameDto.setReleased(LocalDate.of(2100, 1, 1));
        apiGameDto.setGenres(Set.of("Platformer"));
        apiGameDto.setPlatforms(Set.of("PC"));

        // Create fake API game response DTO
        ApiGameResponseDto apiGameResponseDto = new ApiGameResponseDto();
        apiGameResponseDto.setNext(null);
        apiGameResponseDto.setResults(List.of(apiGameDto));

        // Create other fake API DTOs
        ApiGameDetailsDto apiGameDetailsDto = new ApiGameDetailsDto();
        ApiStoresResponseDto apiStoresResponseDto = new ApiStoresResponseDto();
        apiStoresResponseDto.setResults(Collections.emptyList());
        ApiGameStoresResponseDto apiGameStoresResponseDto = new ApiGameStoresResponseDto();
        apiGameStoresResponseDto.setResults(Collections.emptyList());

        // Mock rest API return value
        when(restTemplate.getForEntity(anyString(), eq(ApiGameResponseDto.class)))
        .thenReturn(ResponseEntity.ok(apiGameResponseDto));
        when(restTemplate.getForEntity(anyString(), eq(ApiGameDetailsDto.class)))
        .thenReturn(ResponseEntity.ok(apiGameDetailsDto));
        when(restTemplate.getForEntity(anyString(), eq(ApiStoresResponseDto.class)))
        .thenReturn(ResponseEntity.ok(apiStoresResponseDto));
        when(restTemplate.getForEntity(anyString(), eq(ApiGameStoresResponseDto.class)))
        .thenReturn(ResponseEntity.ok(apiGameStoresResponseDto));

        // Call update games
        service.UpdateGames();

        // Check game was created
        verify(gameRepository, atLeastOnce()).save(any());
    }

    @Test
    void updateGame() {
        // Create fake API game DTO
        ApiGameDto apiGameDto = new ApiGameDto();
        apiGameDto.setReleased(LocalDate.of(2100, 1, 1));

        // Create fake API game response DTO
        ApiGameResponseDto apiGameResponseDto = new ApiGameResponseDto();
        apiGameResponseDto.setNext(null);
        apiGameResponseDto.setResults(List.of(apiGameDto));

        // Create other fake API DTOs
        ApiGameDetailsDto apiGameDetailsDto = new ApiGameDetailsDto();
        ApiStoresResponseDto apiStoresResponseDto = new ApiStoresResponseDto();
        apiStoresResponseDto.setResults(Collections.emptyList());
        ApiGameStoresResponseDto apiGameStoresResponseDto = new ApiGameStoresResponseDto();
        apiGameStoresResponseDto.setResults(Collections.emptyList());

        // Mock rest API return value
        when(restTemplate.getForEntity(anyString(), eq(ApiGameResponseDto.class)))
        .thenReturn(ResponseEntity.ok(apiGameResponseDto));
        when(restTemplate.getForEntity(anyString(), eq(ApiGameDetailsDto.class)))
        .thenReturn(ResponseEntity.ok(apiGameDetailsDto));
        when(restTemplate.getForEntity(anyString(), eq(ApiStoresResponseDto.class)))
        .thenReturn(ResponseEntity.ok(apiStoresResponseDto));
        when(restTemplate.getForEntity(anyString(), eq(ApiGameStoresResponseDto.class)))
        .thenReturn(ResponseEntity.ok(apiGameStoresResponseDto));

        // Mock database lookup
        when(gameRepository.findByRawgGameSlug(any())).thenReturn(List.of(new Game()));

        // Call update games
        service.UpdateGames();

        // Check game was updated
        verify(gameRepository, atLeastOnce()).save(any());
    }

    @Test
    void gameUpToDate() {
        // Create fake API game DTO
        ApiGameDto apiGameDto = new ApiGameDto();
        apiGameDto.setReleased(LocalDate.of(2100, 1, 1));
        apiGameDto.setUpdated(LocalDateTime.of(LocalDate.of(2000, 1, 1), LocalTime.NOON));

        // Create fake API game response DTO
        ApiGameResponseDto apiGameResponseDto = new ApiGameResponseDto();
        apiGameResponseDto.setNext(null);
        apiGameResponseDto.setResults(List.of(apiGameDto));

        // Mock rest API return value
        when(restTemplate.getForEntity(anyString(), eq(ApiGameResponseDto.class)))
        .thenReturn(ResponseEntity.ok(apiGameResponseDto));

        // Create fake API stores dto
        ApiStoresResponseDto apiStoresResponseDto = new ApiStoresResponseDto();
        apiStoresResponseDto.setResults(Collections.emptyList());
        when(restTemplate.getForEntity(anyString(), eq(ApiStoresResponseDto.class)))
        .thenReturn(ResponseEntity.ok(apiStoresResponseDto));

        // Create fake game that was updated since 2000.01.01
        Game fakeGame = new Game();
        fakeGame.setUpdatedAt(LocalDateTime.of(LocalDate.of(2001, 1, 1), LocalTime.NOON));

        // Mock database lookup
        when(gameRepository.findByRawgGameSlug(any())).thenReturn(List.of(fakeGame));

        // Call update games
        service.UpdateGames();

        // Check nothing was saved
        verify(gameRepository, never()).save(any());
    }
    
    @Test
    void clientError() {
        // Create fake API stores dto
        ApiStoresResponseDto apiStoresResponseDto = new ApiStoresResponseDto();
        apiStoresResponseDto.setResults(Collections.emptyList());
        when(restTemplate.getForEntity(anyString(), eq(ApiStoresResponseDto.class)))
        .thenReturn(ResponseEntity.ok(apiStoresResponseDto));

        // Make rest client throw 400 error
        when(restTemplate.getForEntity(anyString(), eq(ApiGameResponseDto.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Call update games
        service.UpdateGames();

        // this test just checks that the program does not crash
    }

    @Test
    void serverError() {
        // Create fake API stores dto
        ApiStoresResponseDto apiStoresResponseDto = new ApiStoresResponseDto();
        apiStoresResponseDto.setResults(Collections.emptyList());
        when(restTemplate.getForEntity(anyString(), eq(ApiStoresResponseDto.class)))
        .thenReturn(ResponseEntity.ok(apiStoresResponseDto));

        // Make rest client throw 500 error
        when(restTemplate.getForEntity(anyString(), eq(ApiGameResponseDto.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        // Call update games
        service.UpdateGames();

        // this test just checks that the program does not crash
    }

    @Test
    void unusualResponseCode() {
        // Create fake API stores dto
        ApiStoresResponseDto apiStoresResponseDto = new ApiStoresResponseDto();
        apiStoresResponseDto.setResults(Collections.emptyList());
        when(restTemplate.getForEntity(anyString(), eq(ApiStoresResponseDto.class)))
        .thenReturn(ResponseEntity.ok(apiStoresResponseDto));

        // Make rest client throw 500 error
        when(restTemplate.getForEntity(anyString(), eq(ApiGameResponseDto.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.FOUND));

        // Call update games
        service.UpdateGames();

        // Check nothing was saved
        verify(gameRepository, never()).save(any());
    }

    @Test
    void jsonParsingError() {
        // Create fake API stores dto
        ApiStoresResponseDto apiStoresResponseDto = new ApiStoresResponseDto();
        apiStoresResponseDto.setResults(Collections.emptyList());
        when(restTemplate.getForEntity(anyString(), eq(ApiStoresResponseDto.class)))
        .thenReturn(ResponseEntity.ok(apiStoresResponseDto));

        // Make rest client throw 400 error
        when(restTemplate.getForEntity(anyString(), eq(ApiGameResponseDto.class)))
                .thenThrow(new RestClientException("Message"));

        // Call update games
        service.UpdateGames();

        // this test just checks that the program does not crash
    }
}
