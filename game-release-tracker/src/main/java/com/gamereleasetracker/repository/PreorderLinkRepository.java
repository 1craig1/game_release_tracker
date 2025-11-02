package com.gamereleasetracker.repository;

import com.gamereleasetracker.model.PreorderLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PreorderLinkRepository extends JpaRepository<PreorderLink, Long> {
    /**
     * Finds a list of all preorder links associated with a specific game,
     * identified by its ID.
     *
     * @param gameId The primary key of the Game entity.
     * @return A list of PreorderLink objects for the specified game.
     */
    List<PreorderLink> findByGameId(Long gameId);
}
