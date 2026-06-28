package com.miniopgg.repository;

import com.miniopgg.entity.Player;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByGameNameIgnoreCaseAndTagLineIgnoreCase(String gameName, String tagLine);

    @EntityGraph(attributePaths = "matches")
    Optional<Player> findByPuuid(String puuid);
}
