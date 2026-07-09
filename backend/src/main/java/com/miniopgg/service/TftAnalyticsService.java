package com.miniopgg.service;

import com.miniopgg.config.RiotApiProperties;
import com.miniopgg.dto.TftMatchResponse;
import com.miniopgg.dto.TftProfileResponse;
import com.miniopgg.dto.TftRankResponse;
import com.miniopgg.dto.TftSummaryResponse;
import com.miniopgg.entity.TftMatch;
import com.miniopgg.entity.TftPlayer;
import com.miniopgg.exception.RiotApiException;
import com.miniopgg.repository.TftPlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TftAnalyticsService {
    private static final int RECENT_MATCH_COUNT = 10;

    private final TftPlayerRepository tftPlayerRepository;
    private final RiotApiService riotApiService;
    private final TftApiService tftApiService;
    private final RiotApiProperties properties;

    public TftAnalyticsService(
            TftPlayerRepository tftPlayerRepository,
            RiotApiService riotApiService,
            TftApiService tftApiService,
            RiotApiProperties properties
    ) {
        this.tftPlayerRepository = tftPlayerRepository;
        this.riotApiService = riotApiService;
        this.tftApiService = tftApiService;
        this.properties = properties;
    }

    @Transactional
    public TftProfileResponse getProfile(String gameName, String tagLine) {
        TftPlayer player = getOrRefreshPlayer(gameName, tagLine);
        return toProfileResponse(player);
    }

    @Transactional
    public TftRankResponse getRank(String gameName, String tagLine) {
        TftPlayer player = getOrRefreshPlayer(gameName, tagLine);
        if (isRankUnavailable(player) || !isFresh(player.getRankUpdatedAt())) {
            TftApiService.TftRank rank;
            try {
                rank = getTftRank(player);
            } catch (RiotApiException ex) {
                if (isUnavailableRankStatus(ex)) {
                    saveUnavailableRank(player);
                    return TftRankResponse.unavailable();
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
            player = tftPlayerRepository.save(player);
        }
        return toRankResponse(player);
    }

    private TftApiService.TftRank getTftRank(TftPlayer player) {
        try {
            return tftApiService.getRank(player.getPuuid());
        } catch (RiotApiException ex) {
            if (ex.getStatusCode() == 400 && player.getSummonerId() != null && !player.getSummonerId().isBlank()) {
                return tftApiService.getRankBySummonerId(player.getSummonerId());
            }
            throw ex;
        }
    }

    private boolean isUnavailableRankStatus(RiotApiException ex) {
        return ex.getStatusCode() == 400 || ex.getStatusCode() == 403;
    }

    private void saveUnavailableRank(TftPlayer player) {
        player.setTier("UNAVAILABLE");
        player.setRank("");
        player.setLeaguePoints(0);
        player.setWins(0);
        player.setLosses(0);
        player.setRankUpdatedAt(Instant.now());
        tftPlayerRepository.save(player);
    }

    @Transactional
    public List<TftMatchResponse> getMatches(String gameName, String tagLine) {
        TftPlayer player = getOrRefreshPlayer(gameName, tagLine);
        player = tftPlayerRepository.findByPuuid(player.getPuuid()).orElse(player);
        if (player.getMatches().size() < RECENT_MATCH_COUNT || !isFresh(player.getMatchesUpdatedAt())) {
            String puuid = player.getPuuid();
            List<String> matchIds = tftApiService.getRecentMatchIds(puuid, RECENT_MATCH_COUNT);
            List<TftMatch> matches = matchIds.stream()
                    .map(matchId -> tftApiService.getMatchDetails(matchId, puuid))
                    .map(this::toEntity)
                    .toList();

            player.replaceMatches(matches);
            player.setMatchesUpdatedAt(Instant.now());
            player = tftPlayerRepository.save(player);
        }

        return sortedMatches(player.getMatches()).stream()
                .map(this::toMatchResponse)
                .toList();
    }

    @Transactional
    public TftSummaryResponse getSummary(String gameName, String tagLine) {
        List<TftMatchResponse> matches = getMatches(gameName, tagLine);
        if (matches.isEmpty()) {
            return new TftSummaryResponse(0, 0, 0, 0, 0, "N/A");
        }

        double topFour = matches.stream().filter(match -> match.placement() <= 4).count();
        double firstPlace = matches.stream().filter(match -> match.placement() == 1).count();
        double averagePlacement = matches.stream()
                .mapToInt(TftMatchResponse::placement)
                .average()
                .orElse(0);
        int bestPlacement = matches.stream()
                .mapToInt(TftMatchResponse::placement)
                .min()
                .orElse(0);
        String mostPlayedUnit = matches.stream()
                .flatMap(match -> match.units().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        return new TftSummaryResponse(
                matches.size(),
                round(averagePlacement),
                round(topFour * 100 / matches.size()),
                round(firstPlace * 100 / matches.size()),
                bestPlacement,
                mostPlayedUnit
        );
    }

    private TftPlayer getOrRefreshPlayer(String gameName, String tagLine) {
        return tftPlayerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase(gameName, tagLine)
                .filter(player -> isFresh(player.getProfileUpdatedAt()))
                .orElseGet(() -> fetchAndSavePlayer(gameName, tagLine));
    }

    private TftPlayer fetchAndSavePlayer(String gameName, String tagLine) {
        RiotApiService.RiotAccount account = riotApiService.searchPlayerByRiotId(gameName, tagLine);
        TftApiService.TftSummoner summoner = tftApiService.getTftSummonerByPuuid(account.puuid());

        TftPlayer player = tftPlayerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase(gameName, tagLine)
                .orElseGet(TftPlayer::new);
        player.setGameName(account.gameName());
        player.setTagLine(account.tagLine());
        player.setPuuid(account.puuid());
        player.setSummonerId(summoner.summonerId());
        player.setSummonerLevel(summoner.summonerLevel());
        player.setProfileUpdatedAt(Instant.now());
        return tftPlayerRepository.save(player);
    }

    private boolean isFresh(Instant updatedAt) {
        if (updatedAt == null) {
            return false;
        }
        return updatedAt.isAfter(Instant.now().minusSeconds(properties.cacheTtlMinutes() * 60));
    }

    private TftProfileResponse toProfileResponse(TftPlayer player) {
        return new TftProfileResponse(
                player.getGameName(),
                player.getTagLine(),
                player.getPuuid(),
                player.getSummonerLevel()
        );
    }

    private TftRankResponse toRankResponse(TftPlayer player) {
        int wins = valueOrZero(player.getWins());
        int losses = valueOrZero(player.getLosses());
        int totalGames = wins + losses;
        double winRate = totalGames == 0 ? 0 : round((double) wins * 100 / totalGames);

        return new TftRankResponse(
                player.getTier() == null ? "UNRANKED" : player.getTier(),
                player.getRank() == null ? "" : player.getRank(),
                valueOrZero(player.getLeaguePoints()),
                wins,
                losses,
                winRate
        );
    }

    private TftMatch toEntity(TftApiService.TftMatchDetails details) {
        TftMatch match = new TftMatch();
        match.setMatchId(details.matchId());
        match.setPlacement(details.placement());
        match.setLevel(details.level());
        match.setGameLength(details.gameLength());
        match.setQueueId(details.queueId());
        match.setGameType(details.gameType());
        match.setPlayedAt(Instant.ofEpochMilli(details.gameDatetime()));
        match.setUnitsCsv(String.join("|", details.units()));
        match.setTraitsCsv(String.join("|", details.traits()));
        return match;
    }

    private TftMatchResponse toMatchResponse(TftMatch match) {
        return new TftMatchResponse(
                match.getMatchId(),
                match.getPlacement(),
                match.getLevel(),
                match.getGameLength(),
                match.getQueueId(),
                match.getGameType(),
                match.getPlayedAt(),
                splitCsv(match.getUnitsCsv()),
                splitCsv(match.getTraitsCsv())
        );
    }

    private List<TftMatch> sortedMatches(List<TftMatch> matches) {
        return matches.stream()
                .collect(Collectors.toMap(
                        TftMatch::getMatchId,
                        Function.identity(),
                        (first, duplicate) -> first,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(TftMatch::getPlayedAt).reversed())
                .toList();
    }

    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\|"))
                .filter(part -> !part.isBlank())
                .toList();
    }

    private boolean isRankUnavailable(TftPlayer player) {
        return "UNAVAILABLE".equals(player.getTier());
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
