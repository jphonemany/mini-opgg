package com.miniopgg.dto;

public record TftProfileResponse(
        String gameName,
        String tagLine,
        String puuid,
        Long summonerLevel
) {
}
