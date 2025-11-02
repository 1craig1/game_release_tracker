package com.gamereleasetracker.repository;

import com.gamereleasetracker.model.Game;
import com.gamereleasetracker.model.GameStatus;
import com.gamereleasetracker.model.Genre;
import com.gamereleasetracker.model.Platform;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

/**
 * A factory for creating JPA Specification instances for the Game entity.
 * The Specification API is a feature in Spring Data JPA used for building dynamic, reusable, and type-safe database queries.
 * Each method in this class returns a reusable Specification that can be combined to build complex, dynamic queries.
 * A Specification is an object that represents a single reusable piece of a database query's WHERE clause.
 * Think of it like a LEGO block for building queries. Each block represents one specific filter or business rule.
 */
@Component
public class GameSpecification {

    /**
     * Creates a specification to filter games by their release status.
     * If the status is null, it returns a predicate that is always true (no filtering).
     *
     * @param status The GameStatus to filter by.
     * @return A Specification for the Game entity.
     */
    public Specification<Game> hasStatus(GameStatus status) {
        // (root, query, cb) -> is a lambda for the Specification's toPredicate method.
        // cb.conjunction() is a predicate that always evaluates to 'true', effectively ignoring this filter.
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    /**
     * Creates a specification for a case-insensitive search on the game's title.
     * If the search term is null or blank, no filtering is applied.
     *
     * @param search The search term to find within game titles.
     * @return A Specification for the Game entity.
     */
    public Specification<Game> titleContains(String search) {
        return (root, query, cb) ->
                search == null || search.isBlank()
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%");
    }

    /**
     * Creates a specification to find games released after a specific date.
     * If the date is null, no filtering is applied.
     *
     * @param afterDate The date to filter games by.
     * @return A Specification for the Game entity.
     */
    public Specification<Game> releasedAfter(LocalDate afterDate) {
        return (root, query, cb) ->
                afterDate == null ? cb.conjunction() : cb.greaterThan(root.get("releaseDate"), afterDate);
    }

    /**
     * Creates a specification to find games belonging to any of the specified genres.
     * This involves a JOIN on the 'genres' collection.
     *
     * @param genres A set of genre names to filter by.
     * @return A Specification for the Game entity.
     */
    public Specification<Game> hasGenres(Set<String> genres) {
        return (root, query, cb) -> {
            if (genres == null || genres.isEmpty()) {
                return cb.conjunction();
            }
            // Using query.distinct(true) prevents duplicate Game results if a game matches multiple genres.
            query.distinct(true);
            Join<Game, Genre> genreJoin = root.join("genres");
            return genreJoin.get("name").in(genres);
        };
    }

    /**
     * Creates a specification to find games available on any of the specified platforms.
     * This involves a JOIN on the 'platforms' collection.
     *
     * @param platforms A set of platform names to filter by.
     * @return A Specification for the Game entity.
     */
    public Specification<Game> hasPlatforms(Set<String> platforms) {
        return (root, query, cb) -> {
            if (platforms == null || platforms.isEmpty()) {
                return cb.conjunction();
            }
            // Using query.distinct(true) prevents duplicate Game results if a game matches multiple genres.
            query.distinct(true);
            Join<Game, Platform> platformJoin = root.join("platforms");
            return platformJoin.get("name").in(platforms);
        };
    }
}