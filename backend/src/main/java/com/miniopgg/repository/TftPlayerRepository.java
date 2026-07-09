package com.miniopgg.repository;

import com.miniopgg.entity.TftPlayer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TftPlayerRepository extends JpaRepository<TftPlayer, Long> {
    Optional<TftPlayer> findByGameNameIgnoreCaseAndTagLineIgnoreCase(String gameName, String tagLine);

    @EntityGraph(attributePaths = "matches")
    Optional<TftPlayer> findByPuuid(String puuid);
}
