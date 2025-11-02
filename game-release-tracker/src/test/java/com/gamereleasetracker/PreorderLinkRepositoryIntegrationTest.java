package com.gamereleasetracker;

import com.gamereleasetracker.model.Game;
import com.gamereleasetracker.model.PreorderLink;
import com.gamereleasetracker.repository.GameRepository;
import com.gamereleasetracker.repository.PreorderLinkRepository;
import com.gamereleasetracker.service.GameUpdateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// @SpringBootTest
// @ActiveProfiles("test") // Ensures this test runs with the 'test' configuration
@Transactional
class PreorderLinkRepositoryIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private GameUpdateService gameUpdateService;

    @Autowired
    private PreorderLinkRepository preorderLinkRepository;

    @Autowired
    private GameRepository gameRepository;

    /**
     * Tests that preorder links are saved correctly and can be found via preorder link ID
     */
    @Test
    @Transactional
    void testSaveAndFindPreorderLink() {
        // --- Setup ---
        Game game = new Game();
        game.setTitle("Totally Legit Game");
        game.setRawgGameSlug("totally-legit-game");
        game.setReleaseDate(LocalDate.now());
        Game savedGame = gameRepository.save(game);

        PreorderLink newLink = new PreorderLink();
        newLink.setStoreName("Steam");
        newLink.setUrl("https://store.steampowered.com/app/12345");
        newLink.setGame(savedGame);

        // --- Action ---
        PreorderLink savedLink = preorderLinkRepository.save(newLink);

        // --- Assertion ---
        assertThat(savedLink).isNotNull();
        assertThat(savedLink.getId()).isGreaterThan(0);

        PreorderLink foundLink = preorderLinkRepository.findById(savedLink.getId()).orElseThrow();
        assertThat(foundLink.getStoreName()).isEqualTo("Steam");
        assertThat(foundLink.getGame().getId()).isEqualTo(savedGame.getId());
    }

    /**
     * Tests the findByGameId method to ensure it only returns links
     * for the specified game.
     */
    @Test
    @Transactional
    void testFindAllPreorderLinksForAGivenGameId() {
        // --- Setup ---
        Game gameA = new Game();
        gameA.setTitle("Game A");
        gameA.setRawgGameSlug("game-a");
        gameA.setReleaseDate(LocalDate.now());
        gameRepository.save(gameA);

        Game gameB = new Game();
        gameB.setTitle("Game B");
        gameB.setRawgGameSlug("game-b");
        gameB.setReleaseDate(LocalDate.now());
        gameRepository.save(gameB);

        PreorderLink linkA1 = new PreorderLink();
        linkA1.setStoreName("Steam");
        linkA1.setUrl("url1");
        linkA1.setGame(gameA);

        PreorderLink linkA2 = new PreorderLink();
        linkA2.setStoreName("GOG");
        linkA2.setUrl("url2");
        linkA2.setGame(gameA);

        PreorderLink linkB1 = new PreorderLink();
        linkB1.setStoreName("Epic Store");
        linkB1.setUrl("url3");
        linkB1.setGame(gameB);

        preorderLinkRepository.saveAll(Arrays.asList(linkA1, linkA2, linkB1));
        // --- Action ---
        List<PreorderLink> foundLinks = preorderLinkRepository.findByGameId(gameA.getId());

        // --- Assertion ---
        assertThat(foundLinks).hasSize(2);
        assertThat(foundLinks).extracting(PreorderLink::getStoreName)
                .containsExactlyInAnyOrder("Steam", "GOG");
    }
}