package com.miniopgg.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tft_matches")
public class TftMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private TftPlayer player;

    @Column(nullable = false)
    private String matchId;

    private int placement;
    private int level;
    private long gameLength;
    private int queueId;
    private String gameType;
    private Instant playedAt;

    @Column(length = 1000)
    private String unitsCsv;

    @Column(length = 1000)
    private String traitsCsv;

    public Long getId() {
        return id;
    }

    public TftPlayer getPlayer() {
        return player;
    }

    public void setPlayer(TftPlayer player) {
        this.player = player;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public int getPlacement() {
        return placement;
    }

    public void setPlacement(int placement) {
        this.placement = placement;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getGameLength() {
        return gameLength;
    }

    public void setGameLength(long gameLength) {
        this.gameLength = gameLength;
    }

    public int getQueueId() {
        return queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public Instant getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(Instant playedAt) {
        this.playedAt = playedAt;
    }

    public String getUnitsCsv() {
        return unitsCsv;
    }

    public void setUnitsCsv(String unitsCsv) {
        this.unitsCsv = unitsCsv;
    }

    public String getTraitsCsv() {
        return traitsCsv;
    }

    public void setTraitsCsv(String traitsCsv) {
        this.traitsCsv = traitsCsv;
    }
}
