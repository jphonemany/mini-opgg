package com.miniopgg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.miniopgg.config.RiotApiProperties;
import com.miniopgg.exception.PlayerNotFoundException;
import com.miniopgg.exception.RiotApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.node.MissingNode;
import java.util.ArrayList;
import java.util.List;

@Service
public class RiotApiService {
    private final RiotApiProperties properties;
    private final RestClient restClient;

    public RiotApiService(RiotApiProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    public RiotAccount searchPlayerByRiotId(String gameName, String tagLine) {
        JsonNode account;
        try {
            account = getJson(regionalBaseUrl()
                    + "/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}", gameName, tagLine);
        } catch (RiotApiException ex) {
            if (ex.getStatusCode() == 404) {
                throw new PlayerNotFoundException(gameName, tagLine);
            }
            throw ex;
        }
        return new RiotAccount(
                account.path("gameName").asText(gameName),
                account.path("tagLine").asText(tagLine),
                account.path("puuid").asText()
        );
    }

    public RiotSummoner getSummonerByPuuid(String puuid) {
        JsonNode summoner = getJson(platformBaseUrl()
                + "/lol/summoner/v4/summoners/by-puuid/{puuid}", puuid);
        return new RiotSummoner(
                summoner.path("id").asText(),
                summoner.path("summonerLevel").asLong()
        );
    }

    public RiotRank getSoloQueueRank(String summonerId) {
        JsonNode entries = getJson(platformBaseUrl()
                + "/lol/league/v4/entries/by-summoner/{summonerId}", summonerId);
        for (JsonNode entry : entries) {
            if ("RANKED_SOLO_5x5".equals(entry.path("queueType").asText())) {
                return new RiotRank(
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
                + "/lol/match/v5/matches/by-puuid/{puuid}/ids?start=0&count={count}", puuid, count);
        List<String> matchIds = new ArrayList<>();
        ids.forEach(id -> matchIds.add(id.asText()));
        return matchIds;
    }

    public RiotMatch getMatchDetails(String matchId, String puuid) {
        JsonNode match = getJson(regionalBaseUrl() + "/lol/match/v5/matches/{matchId}", matchId);
        JsonNode info = match.path("info");
        JsonNode participant = findParticipant(info.path("participants"), puuid);
        if (participant.isMissingNode()) {
            throw new RiotApiException("Player was not present in match " + matchId + ".", 502);
        }

        int cs = participant.path("totalMinionsKilled").asInt()
                + participant.path("neutralMinionsKilled").asInt();

        return new RiotMatch(
                match.path("metadata").path("matchId").asText(matchId),
                participant.path("championName").asText(),
                participant.path("kills").asInt(),
                participant.path("deaths").asInt(),
                participant.path("assists").asInt(),
                participant.path("win").asBoolean(),
                cs,
                info.path("gameDuration").asLong(),
                info.path("gameMode").asText(),
                info.path("gameEndTimestamp").asLong()
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
            throw new RiotApiException("Riot API resource was not found.", 404);
        } catch (HttpClientErrorException.TooManyRequests ex) {
            throw new RiotApiException("Riot API rate limit reached. Wait a moment and try again.", 429);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new RiotApiException("Riot API request failed with status " + ex.getStatusCode() + ".", ex.getStatusCode().value());
        } catch (ResourceAccessException ex) {
            throw new RiotApiException("Riot API request timed out. Try again in a moment.", 504);
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

    public record RiotAccount(String gameName, String tagLine, String puuid) {
    }

    public record RiotSummoner(String summonerId, long summonerLevel) {
    }

    public record RiotRank(String tier, String rank, int leaguePoints, int wins, int losses) {
    }

    public record RiotMatch(
            String matchId,
            String championName,
            int kills,
            int deaths,
            int assists,
            boolean win,
            int cs,
            long gameDuration,
            String gameMode,
            long gameEndTimestamp
    ) {
    }
}
