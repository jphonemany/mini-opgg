package com.miniopgg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.miniopgg.config.RiotApiProperties;
import com.miniopgg.exception.RiotApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class TftApiService {
    private final RiotApiProperties properties;
    private final RestClient restClient;

    public TftApiService(RiotApiProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    public TftSummoner getTftSummonerByPuuid(String puuid) {
        JsonNode summoner = getJson(platformBaseUrl()
                + "/tft/summoner/v1/summoners/by-puuid/{puuid}", puuid);
        return new TftSummoner(
                summoner.path("id").asText(),
                summoner.path("summonerLevel").asLong()
        );
    }

    public TftRank getRank(String puuid) {
        JsonNode entries = getJson(platformBaseUrl()
                + "/tft/league/v1/by-puuid/{puuid}", puuid);
        return findRankedTftEntry(entries);
    }

    public TftRank getRankBySummonerId(String summonerId) {
        JsonNode entries = getJson(platformBaseUrl()
                + "/tft/league/v1/entries/by-summoner/{summonerId}", summonerId);
        return findRankedTftEntry(entries);
    }

    private TftRank findRankedTftEntry(JsonNode entries) {
        for (JsonNode entry : entries) {
            if ("RANKED_TFT".equals(entry.path("queueType").asText())) {
                return new TftRank(
                        entry.path("tier").asText("UNRANKED"),
                        entry.path("rank").asText(""),
                        entry.path("leaguePoints").asInt(),
                        entry.path("wins").asInt(),
                        entry.path("losses").asInt()
                );
            }
        }
        return null;
    }

    public List<String> getRecentMatchIds(String puuid, int count) {
        JsonNode ids = getJson(regionalBaseUrl()
                + "/tft/match/v1/matches/by-puuid/{puuid}/ids?start=0&count={count}", puuid, count);
        List<String> matchIds = new ArrayList<>();
        ids.forEach(id -> matchIds.add(id.asText()));
        return matchIds;
    }

    public TftMatchDetails getMatchDetails(String matchId, String puuid) {
        JsonNode match = getJson(regionalBaseUrl() + "/tft/match/v1/matches/{matchId}", matchId);
        JsonNode info = match.path("info");
        JsonNode participant = findParticipant(info.path("participants"), puuid);
        if (participant.isMissingNode()) {
            throw new RiotApiException("Player was not present in TFT match " + matchId + ".", 502);
        }

        List<String> units = new ArrayList<>();
        participant.path("units").forEach(unit -> units.add(cleanName(unit.path("character_id").asText())));

        List<String> traits = new ArrayList<>();
        participant.path("traits").forEach(trait -> {
            if (trait.path("tier_current").asInt() > 0) {
                traits.add(cleanName(trait.path("name").asText()));
            }
        });

        return new TftMatchDetails(
                match.path("metadata").path("match_id").asText(matchId),
                participant.path("placement").asInt(),
                participant.path("level").asInt(),
                Math.round(info.path("game_length").asDouble()),
                info.path("queue_id").asInt(),
                info.path("tft_game_type").asText("standard"),
                info.path("game_datetime").asLong(),
                units,
                traits
        );
    }

    private JsonNode findParticipant(JsonNode participants, String puuid) {
        for (JsonNode participant : participants) {
            if (puuid.equals(participant.path("puuid").asText())) {
                return participant;
            }
        }
        return MissingNode.getInstance();
    }

    private JsonNode getJson(String url, Object... uriVariables) {
        ensureApiKeyConfigured();
        try {
            return restClient.get()
                    .uri(url, uriVariables)
                    .header("X-Riot-Token", properties.apiKey())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RiotApiException("Riot TFT API resource was not found.", 404);
        } catch (HttpClientErrorException.TooManyRequests ex) {
            throw new RiotApiException("Riot API rate limit reached. Wait a moment and try again.", 429);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new RiotApiException("Riot TFT API request failed with status " + ex.getStatusCode() + ".", ex.getStatusCode().value());
        } catch (ResourceAccessException ex) {
            throw new RiotApiException("Riot TFT API request timed out. Try again in a moment.", 504);
        }
    }

    private void ensureApiKeyConfigured() {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new RiotApiException("RIOT_API_KEY is not configured.", HttpStatus.SERVICE_UNAVAILABLE.value());
        }
    }

    private String regionalBaseUrl() {
        return "https://" + properties.regionalRoute() + ".api.riotgames.com";
    }

    private String platformBaseUrl() {
        return "https://" + properties.platformRoute() + ".api.riotgames.com";
    }

    private String cleanName(String value) {
        return value
                .replace("TFT_", "")
                .replaceAll("^Set\\d+_", "")
                .replace('_', ' ');
    }

    public record TftSummoner(String summonerId, long summonerLevel) {
    }

    public record TftRank(String tier, String rank, int leaguePoints, int wins, int losses) {
    }

    public record TftMatchDetails(
            String matchId,
            int placement,
            int level,
            long gameLength,
            int queueId,
            String gameType,
            long gameDatetime,
            List<String> units,
            List<String> traits
    ) {
    }
}
