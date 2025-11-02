package com.gamereleasetracker;

import com.gamereleasetracker.dto.PreorderLinkDto;
import com.gamereleasetracker.exception.NotFoundException;
import com.gamereleasetracker.model.Game;
import com.gamereleasetracker.model.PreorderLink;
import com.gamereleasetracker.repository.GameRepository;
import com.gamereleasetracker.repository.PreorderLinkRepository;
import com.gamereleasetracker.service.PreorderLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class PreorderLinkServiceUnitTest {

    @Mock
    private PreorderLinkRepository preorderLinkRepository;

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private PreorderLinkService preorderLinkService;

    private Game game;
    private PreorderLink preorderLink;
    private PreorderLinkDto preorderLinkDto;

    @BeforeEach
    void setUp() {
        game = new Game();
        game.setId(101L);
        game.setTitle("Stellar Blade");

        preorderLink = new PreorderLink();
        preorderLink.setId(1L);
        preorderLink.setGame(game);
        preorderLink.setStoreName("PlayStation Store");
        preorderLink.setUrl("https://www.playstation.com/en-au/games/stellar-blade/");

        preorderLinkDto = new PreorderLinkDto(1L, 101L, "PlayStation Store", "https://www.playstation.com/en-au/games/stellar-blade/");
    }

    @Test
    void testGetPreorderLinksByGameId() {
        // --- Setup ---
        when(preorderLinkRepository.findByGameId(game.getId())).thenReturn(List.of(preorderLink));

        // --- Action ---
        Set<PreorderLinkDto> result = preorderLinkService.getPreorderLinksByGameId(game.getId());

        // --- Assertion ---
        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().storeName()).isEqualTo("PlayStation Store");
    }

    @Test
    void testGetPreorderLinksByGameIdReturnsEmptySet() {
        // --- Setup ---
        when(preorderLinkRepository.findByGameId(anyLong())).thenReturn(Collections.emptyList());

        // --- Action ---
        Set<PreorderLinkDto> result = preorderLinkService.getPreorderLinksByGameId(99L);

        // --- Assertion ---
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void testAddPreorderLinkSuccess() {
        // --- Setup ---
        when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
        when(preorderLinkRepository.save(any(PreorderLink.class))).thenReturn(preorderLink);
        PreorderLinkDto createDto = new PreorderLinkDto(null, game.getId(), "PS Store", "http://url.com");

        // --- Action ---
        preorderLinkService.addPreorderLink(createDto);

        // --- Assertion ---
        ArgumentCaptor<PreorderLink> captor = ArgumentCaptor.forClass(PreorderLink.class);
        verify(preorderLinkRepository).save(captor.capture());
        PreorderLink savedLink = captor.getValue();

        assertThat(savedLink.getGame()).isEqualTo(game);
    }

    @Test
    void testAddPreorderLinkThrowsNotFoundExceptionForGame() {
        // --- Setup ---
        when(gameRepository.findById(anyLong())).thenReturn(Optional.empty());
        PreorderLinkDto createDto = new PreorderLinkDto(null, 999L, "Store", "url");

        // --- Action & Assertion ---
        assertThrows(NotFoundException.class, () -> preorderLinkService.addPreorderLink(createDto));
        verify(preorderLinkRepository, never()).save(any());
    }

    @Test
    void testDeletePreorderLinkSuccess() {
        // --- Setup ---
        when(preorderLinkRepository.findById(preorderLink.getId())).thenReturn(Optional.of(preorderLink));
        doNothing().when(preorderLinkRepository).delete(any(PreorderLink.class));

        // --- Action ---
        preorderLinkService.deletePreorderLink(preorderLinkDto);

        // --- Assertion ---
        verify(preorderLinkRepository, times(1)).delete(preorderLink);
    }

    @Test
    void testDeletePreorderLinkThrowsNotFoundException() {
        // --- Setup ---
        when(preorderLinkRepository.findById(anyLong())).thenReturn(Optional.empty());

        // --- Action & Assertion ---
        assertThrows(NotFoundException.class, () -> preorderLinkService.deletePreorderLink(preorderLinkDto));
        verify(preorderLinkRepository, never()).delete(any());
    }

    @Test
    void testUpdatePreorderLinkUrlSuccess() {
        // --- Setup ---
        Game newGame = new Game();
        newGame.setId(102L);
        PreorderLinkDto updateDto = new PreorderLinkDto(1L, 102L, "New Store", "http://new.url");

        when(preorderLinkRepository.findById(1L)).thenReturn(Optional.of(preorderLink));
        when(gameRepository.findById(102L)).thenReturn(Optional.of(newGame));
        when(preorderLinkRepository.save(any(PreorderLink.class))).thenAnswer(inv -> inv.getArgument(0));

        // --- Action ---
        PreorderLinkDto result = preorderLinkService.updatePreorderLinkUrl(updateDto);

        // --- Assertion ---
        assertThat(result.storeName()).isEqualTo("New Store");
        assertThat(result.url()).isEqualTo("http://new.url");
        assertThat(result.gameId()).isEqualTo(102L);

        ArgumentCaptor<PreorderLink> captor = ArgumentCaptor.forClass(PreorderLink.class);
        verify(preorderLinkRepository).save(captor.capture());
        assertThat(captor.getValue().getGame().getId()).isEqualTo(102L);
    }

    @Test
    void testUpdatePreorderLinkThrowsNotFoundExceptionForLink() {
        // --- Setup ---
        when(preorderLinkRepository.findById(anyLong())).thenReturn(Optional.empty());

        // --- Action & Assertion ---
        assertThrows(NotFoundException.class, () -> preorderLinkService.updatePreorderLinkUrl(preorderLinkDto));
    }

    @Test
    void testUpdatePreorderLinkThrowsNotFoundExceptionForGame() {
        // --- Setup ---
        PreorderLinkDto updateDto = new PreorderLinkDto(1L, 999L, null, null);
        when(preorderLinkRepository.findById(1L)).thenReturn(Optional.of(preorderLink));
        when(gameRepository.findById(999L)).thenReturn(Optional.empty());

        // --- Action & Assertion ---
        assertThrows(NotFoundException.class, () -> preorderLinkService.updatePreorderLinkUrl(updateDto));
        verify(preorderLinkRepository, never()).save(any());
    }

    @Test
    void testGetPreorderLinkByIdSuccess() {
        // --- Setup ---
        when(preorderLinkRepository.findById(1L)).thenReturn(Optional.of(preorderLink));

        // --- Action ---
        PreorderLinkDto result = preorderLinkService.getPreorderLinkById(1L);

        // --- Assertion ---
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.storeName()).isEqualTo(preorderLink.getStoreName());
    }

    @Test
    void testGetPreorderLinkByIdThrowsNotFoundException() {
        // --- Setup ---
        when(preorderLinkRepository.findById(anyLong())).thenReturn(Optional.empty());

        // --- Action & Assertion ---
        assertThrows(NotFoundException.class, () -> preorderLinkService.getPreorderLinkById(99L));
    }

    @Test
    void testGetAllPreorderLinks() {
        // --- Setup ---
        PreorderLink anotherLink = new PreorderLink();
        anotherLink.setId(2L);
        anotherLink.setGame(game);
        anotherLink.setStoreName("Steam");

        when(preorderLinkRepository.findAll()).thenReturn(List.of(preorderLink, anotherLink));

        // --- Action ---
        Set<PreorderLinkDto> result = preorderLinkService.getAllPreorderLinks();

        // --- Assertion ---
        assertThat(result).hasSize(2);
    }

    @Test
    void testGetAllPreorderLinksReturnsEmptySet() {
        // --- Setup ---
        when(preorderLinkRepository.findAll()).thenReturn(Collections.emptyList());

        // --- Action ---
        Set<PreorderLinkDto> result = preorderLinkService.getAllPreorderLinks();

        // --- Assertion ---
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }
}
