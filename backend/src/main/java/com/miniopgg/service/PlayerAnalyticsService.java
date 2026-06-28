package com.miniopgg.service;

import com.miniopgg.config.RiotApiProperties;
import com.miniopgg.dto.MatchSummaryResponse;
import com.miniopgg.dto.PerformanceSummaryResponse;
import com.miniopgg.dto.PlayerProfileResponse;
import com.miniopgg.dto.RankedInfoResponse;
import com.miniopgg.entity.Player;
import com.miniopgg.entity.PlayerMatch;
import com.miniopgg.exception.RiotApiException;
import com.miniopgg.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlayerAnalyticsService {
    private static final int RECENT_MATCH_COUNT = 10;

    private final PlayerRepository playerRepository;
    private final RiotApiService riotApiService;
    private final RiotApiProperties properties;

    public PlayerAnalyticsService(
            PlayerRepository playerRepository,
            RiotApiService riotApiService,
            RiotApiProperties properties
    ) {
        this.playerRepository = playerRepository;
        this.riotApiService = riotApiService;
        this.properties = properties;
    }

    @Transactional
    public PlayerProfileResponse getProfile(String gameName, String tagLine) {
        Player player = getOrRefreshPlayer(gameName, tagLine);
        return toProfileResponse(player);
    }

    @Transactional
    public RankedInfoResponse getRank(String gameName, String tagLine) {
        Player player = getOrRefreshPlayer(gameName, tagLine);
        if (isRankUnavailable(player) || !isFresh(player.getRankUpdatedAt())) {
            RiotApiService.RiotRank rank;
            try {
                rank = riotApiService.getSoloQueueRank(player.getSummonerId());
            } catch (RiotApiException ex) {
                if (ex.getStatusCode() == 403) {
                    player.setTier("UNAVAILABLE");
                    player.setRank("");
                    player.setLeaguePoints(0);
                    player.setWins(0);
                    player.setLosses(0);
                    player.setRankUpdatedAt(Instant.now());
                    playerRepository.save(player);
                    return RankedInfoResponse.unavailable();
                }
                throw ex;
            }
            if (rank == null) {
                player.setTier("UNRANKED");
                player.setRank("");
                player.setLeaguePoints(0);
                player.setWins(0);
                player.setLosses(0);
            } else {
                player.setTier(rank.tier());
                player.setRank(rank.rank());
                player.setLeaguePoints(rank.leaguePoints());
                player.setWins(rank.wins());
                player.setLosses(rank.losses());
            }
            player.setRankUpdatedAt(Instant.now());
            player = playerRepository.save(player);
        }
        return toRankedResponse(player);
    }

    @Transactional
    public List<MatchSummaryResponse> getMatches(String gameName, String tagLine) {
        Player player = getOrRefreshPlayer(gameName, tagLine);
        player = playerRepository.findByPuuid(player.getPuuid()).orElse(player);
        if (player.getMatches().size() < RECENT_MATCH_COUNT || !isFresh(player.getMatchesUpdatedAt())) {
            String puuid = player.getPuuid();
            List<String> matchIds = riotApiService.getRecentMatchIds(puuid, RECENT_MATCH_COUNT);
            List<PlayerMatch> matches = matchIds.stream()
                    .map(matchId -> riotApiService.getMatchDetails(matchId, puuid))
                    .map(this::toEntity)
                    .toList();

            player.replaceMatches(matches);
            player.setMatchesUpdatedAt(Instant.now());
            player = playerRepository.save(player);
        }

        return sortedMatches(player.getMatches()).stream()
                .map(this::toMatchResponse)
                .toList();
    }

    @Transactional
    public PerformanceSummaryResponse getSummary(String gameName, String tagLine) {
        List<MatchSummaryResponse> matches = getMatches(gameName, tagLine);
        if (matches.isEmpty()) {
            return new PerformanceSummaryResponse(0, 0, 0, 0, 0, 0, "N/A");
        }

        double wins = matches.stream().filter(MatchSummaryResponse::win).count();
        double averageKills = average(matches, MatchSummaryResponse::kills);
        double averageDeaths = average(matches, MatchSummaryResponse::deaths);
        double averageAssists = average(matches, MatchSummaryResponse::assists);
        double averageKda = matches.stream()
                .mapToDouble(match -> (double) (match.kills() + match.assists()) / Math.max(1, match.deaths()))
                .average()
                .orElse(0);

        String mostPlayedChampion = matches.stream()
                .collect(Collectors.groupingBy(MatchSummaryResponse::championName, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        return new PerformanceSummaryResponse(
                matches.size(),
                round(wins * 100 / matches.size()),
                round(averageKills),
                round(averageDeaths),
                round(averageAssists),
                round(averageKda),
                mostPlayedChampion
        );
    }

    private Player getOrRefreshPlayer(String gameName, String tagLine) {
        return playerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase(gameName, tagLine)
                .filter(player -> isFresh(player.getProfileUpdatedAt()))
                .orElseGet(() -> fetchAndSavePlayer(gameName, tagLine));
    }

    private Player fetchAndSavePlayer(String gameName, String tagLine) {
        RiotApiService.RiotAccount account = riotApiService.searchPlayerByRiotId(gameName, tagLine);
        RiotApiService.RiotSummoner summoner = riotApiService.getSummonerByPuuid(account.puuid());

        Player player = playerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase(gameName, tagLine)
                .orElseGet(Player::new);
        player.setGameName(account.gameName());
        player.setTagLine(account.tagLine());
        player.setPuuid(account.puuid());
        player.setSummonerId(summoner.summonerId());
        player.setSummonerLevel(summoner.summonerLevel());
        player.setProfileUpdatedAt(Instant.now());
        return playerRepository.save(player);
    }

    private boolean isFresh(Instant updatedAt) {
        if (updatedAt == null) {
            return false;
        }
        return updatedAt.isAfter(Instant.now().minusSeconds(properties.cacheTtlMinutes() * 60));
    }

    private PlayerProfileResponse toProfileResponse(Player player) {
        return new PlayerProfileResponse(
                player.getGameName(),
                player.getTagLine(),
                player.getPuuid(),
                player.getSummonerLevel()
        );
    }

    private RankedInfoResponse toRankedResponse(Player player) {
        int wins = valueOrZero(player.getWins());
        int losses = valueOrZero(player.getLosses());
        int totalGames = wins + losses;
        double winRate = totalGames == 0 ? 0 : round((double) wins * 100 / totalGames);

        return new RankedInfoResponse(
                player.getTier() == null ? "UNRANKED" : player.getTier(),
                player.getRank() == null ? "" : player.getRank(),
                valueOrZero(player.getLeaguePoints()),
                wins,
                losses,
                winRate
        );
    }

    private PlayerMatch toEntity(RiotApiService.RiotMatch riotMatch) {
        PlayerMatch match = new PlayerMatch();
        match.setMatchId(riotMatch.matchId());
        match.setChampionName(riotMatch.championName());
        match.setKills(riotMatch.kills());
        match.setDeaths(riotMatch.deaths());
        match.setAssists(riotMatch.assists());
        match.setWin(riotMatch.win());
        match.setCs(riotMatch.cs());
        match.setGameDuration(riotMatch.gameDuration());
        match.setGameMode(riotMatch.gameMode());
        match.setPlayedAt(Instant.ofEpochMilli(riotMatch.gameEndTimestamp()));
        return match;
    }

    private MatchSummaryResponse toMatchResponse(PlayerMatch match) {
        return new MatchSummaryResponse(
                match.getMatchId(),
                match.getChampionName(),
                match.getKills(),
                match.getDeaths(),
                match.getAssists(),
                match.isWin(),
                match.getCs(),
                match.getGameDuration(),
                match.getGameMode(),
                match.getPlayedAt()
        );
    }

    private List<PlayerMatch> sortedMatches(List<PlayerMatch> matches) {
        return matches.stream()
                .collect(Collectors.toMap(
                        PlayerMatch::getMatchId,
                        Function.identity(),
                        (first, duplicate) -> first,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(PlayerMatch::getPlayedAt).reversed())
                .toList();
    }

    private boolean isRankUnavailable(Player player) {
        return "UNAVAILABLE".equals(player.getTier());
    }

    private double average(List<MatchSummaryResponse> matches, Function<MatchSummaryResponse, Integer> metric) {
        return matches.stream().mapToInt(match -> metric.apply(match)).average().orElse(0);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
