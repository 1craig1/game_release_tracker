package com.gamereleasetracker.repository;

import com.gamereleasetracker.model.Platform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformRepository extends JpaRepository<Platform, Integer> {

    Optional<Platform> findByName(String name);
}