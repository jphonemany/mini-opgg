package com.miniopgg.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "players",
        uniqueConstraints = @UniqueConstraint(columnNames = {"game_name", "tag_line"})
)
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_name", nullable = false)
    private String gameName;

    @Column(name = "tag_line", nullable = false)
    private String tagLine;

    @Column(nullable = false, unique = true, length = 128)
    private String puuid;

    @Column(name = "summoner_id")
    private String summonerId;

    @Column(name = "summoner_level")
    private Long summonerLevel;

    @Column(name = "profile_updated_at")
    private Instant profileUpdatedAt;

    @Column(name = "rank_updated_at")
    private Instant rankUpdatedAt;

    private String tier;

    @Column(name = "division_rank")
    private String rank;

    private Integer leaguePoints;
    private Integer wins;
    private Integer losses;

    @Column(name = "matches_updated_at")
    private Instant matchesUpdatedAt;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerMatch> matches = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getTagLine() {
        return tagLine;
    }

    public void setTagLine(String tagLine) {
        this.tagLine = tagLine;
    }

    public String getPuuid() {
        return puuid;
    }

    public void setPuuid(String puuid) {
        this.puuid = puuid;
    }

    public String getSummonerId() {
        return summonerId;
    }

    public void setSummonerId(String summonerId) {
        this.summonerId = summonerId;
    }

    public Long getSummonerLevel() {
        return summonerLevel;
    }

    public void setSummonerLevel(Long summonerLevel) {
        this.summonerLevel = summonerLevel;
    }

    public Instant getProfileUpdatedAt() {
        return profileUpdatedAt;
    }

    public void setProfileUpdatedAt(Instant profileUpdatedAt) {
        this.profileUpdatedAt = profileUpdatedAt;
    }

    public Instant getRankUpdatedAt() {
        return rankUpdatedAt;
    }

    public void setRankUpdatedAt(Instant rankUpdatedAt) {
        this.rankUpdatedAt = rankUpdatedAt;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public Integer getLeaguePoints() {
        return leaguePoints;
    }

    public void setLeaguePoints(Integer leaguePoints) {
        this.leaguePoints = leaguePoints;
    }

    public Integer getWins() {
        return wins;
    }

    public void setWins(Integer wins) {
        this.wins = wins;
    }

    public Integer getLosses() {
        return losses;
    }

    public void setLosses(Integer losses) {
        this.losses = losses;
    }

    public Instant getMatchesUpdatedAt() {
        return matchesUpdatedAt;
    }

    public void setMatchesUpdatedAt(Instant matchesUpdatedAt) {
        this.matchesUpdatedAt = matchesUpdatedAt;
    }

    public List<PlayerMatch> getMatches() {
        return matches;
    }

    public void replaceMatches(List<PlayerMatch> newMatches) {
        matches.clear();
        newMatches.forEach(match -> {
            match.setPlayer(this);
            matches.add(match);
        });
    }
}
