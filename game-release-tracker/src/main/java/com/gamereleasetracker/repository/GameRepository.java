package com.gamereleasetracker.repository;

import com.gamereleasetracker.model.Game;
import com.gamereleasetracker.model.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/* Marks this interface as a Spring Data repository.
This tells Spring to create an implementation for it and to automatically
translate database errors into standard Spring exceptions. */
@Repository
public interface GameRepository extends JpaRepository<Game, Long>, JpaSpecificationExecutor<Game> {

    /**
     * Finds all games scheduled for release after a given date.
     * This is a derived query method; Spring Data JPA automatically creates the
     * database query by parsing the method name:
     * - findBy...: The query's WHERE clause.
     * - ReleaseDateAfter: Finds records where the 'releaseDate' field is > the given date.
     * - OrderByReleaseDateAsc: Sorts the results by 'releaseDate' in ascending order.
     *
     * @param currentDate The current date and time
     * @return A list of matching games.
     */
    List<Game> findByReleaseDateAfterOrderByReleaseDateAsc(LocalDate currentDate);

    /**
     * Finds a list of games where the title contains the given search term, ignoring case.
     *
     * @param searchTerm The partial or full title to search for.
     * @return A list of matching games.
     */
    List<Game> findByTitleContainingIgnoreCase(String searchTerm);

    /**
     * Finds a list of games based on their release status.
     *
     * @param status The status to filter by (e.g., UPCOMING, RELEASED).
     * @return A list of matching games.
     */
    List<Game> findByStatus(GameStatus status);

    /**
     * Finds all games that are available on at least one of the platforms
     * specified in the provided set.
     *
     * @param platformNames A set of platform names to search for (e.g., {"PC", "PlayStation"}).
     * @return A list of matching games.
     */
    List<Game> findByPlatforms_NameIn(Set<String> platformNames);

    /**
     * Finds all games that have at least one genre matching any of the names
     * in the provided set.
     *
     * @param genreNames A set of genre names to search for (e.g., {"Action", "RPG"}).
     * @return A list of matching games.
     */
    List<Game> findByGenres_NameIn(Set<String> genreNames);
    List<Game> findByGenres_Name(String name);

    /**
     * Finds game by its RAWG Game Slug.
     * 
     * @param rawgGameSlug The RAWG Game Slug of the game
     * @return A list containing matching game.
     */
    List<Game> findByRawgGameSlug(String rawgGameSlug);

}
