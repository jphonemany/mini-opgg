package com.miniopgg.dto;

public record TftRankResponse(
        String tier,
        String rank,
        int leaguePoints,
        int wins,
        int losses,
        double winRate
) {
    public static TftRankResponse unranked() {
        return new TftRankResponse("UNRANKED", "", 0, 0, 0, 0);
    }

    public static TftRankResponse unavailable() {
        return new TftRankResponse("UNAVAILABLE", "", 0, 0, 0, 0);
    }
}
