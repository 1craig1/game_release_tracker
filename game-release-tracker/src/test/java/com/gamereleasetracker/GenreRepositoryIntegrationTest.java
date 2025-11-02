package com.gamereleasetracker;

import com.gamereleasetracker.model.Genre;
import com.gamereleasetracker.repository.GenreRepository;
import com.gamereleasetracker.service.GameUpdateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

// @SpringBootTest
// @ActiveProfiles("test") // Ensures this test runs with the 'test' configuration
@Transactional
class GenreRepositoryIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private GameUpdateService gameUpdateService;

    @Autowired
    private GenreRepository genreRepository;

    /**
     * Tests that a Genre entity can be saved and then retrieved from the database,
     * confirming its name is persisted correctly.
     */
    @Test
    @Transactional
    void testSaveAndFindGenre() {
        // --- Setup ---
        Genre newGenre = new Genre();
        newGenre.setName("Strategy");

        // --- Action ---
        Genre savedGenre = genreRepository.save(newGenre);

        // --- Assertion ---
        Genre foundGenre = genreRepository.findById(savedGenre.getId()).orElseThrow();
        assertThat(foundGenre.getName()).isEqualTo("Strategy");
    }

    /**
     * Verifies that the database's unique constraint on the 'name' column is working.
     * It expects a DataIntegrityViolationException when trying to save a genre
     * with a name that already exists.
     */
    @Test
    @Transactional
    void testThrowExceptionWhenSavingDuplicateGenreName() {
        // --- Setup ---
        Genre genre1 = new Genre();
        genre1.setName("RPG");
        genreRepository.save(genre1);

        Genre genre2 = new Genre();
        genre2.setName("RPG"); // Duplicate name

        // --- Action & Assertion ---
        // Asserts that executing the save operation throws the expected exception.
        assertThrows(DataIntegrityViolationException.class, () -> {
            genreRepository.saveAndFlush(genre2);
        });
    }

    /**
     * Tests the findAll method to ensure it retrieves all saved Genre entities
     * from the database.
     */
    @Test
    @Transactional
    void testFindAllGenres() {
        // --- Setup ---
        Genre genre1 = new Genre();
        genre1.setName("Action");
        genreRepository.save(genre1);

        Genre genre2 = new Genre();
        genre2.setName("Adventure");
        genreRepository.save(genre2);

        // --- Action ---
        List<Genre> genres = genreRepository.findAll();

        // --- Assertion ---
        assertThat(genres).hasSize(2);
        assertThat(genres).extracting(Genre::getName)
                .containsExactlyInAnyOrder("Action", "Adventure");
    }
}