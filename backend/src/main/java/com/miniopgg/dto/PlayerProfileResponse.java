package com.miniopgg.dto;

public record PlayerProfileResponse(
        String gameName,
        String tagLine,
        String puuid,
        Long summonerLevel
) {
}
