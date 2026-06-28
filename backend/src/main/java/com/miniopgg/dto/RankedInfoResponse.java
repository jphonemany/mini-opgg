package com.miniopgg.dto;

public record RankedInfoResponse(
        String tier,
        String rank,
        int leaguePoints,
        int wins,
        int losses,
        double winRate
) {
    public static RankedInfoResponse unranked() {
        return new RankedInfoResponse("UNRANKED", "", 0, 0, 0, 0);
    }

    public static RankedInfoResponse unavailable() {
        return new RankedInfoResponse("UNAVAILABLE", "", 0, 0, 0, 0);
    }
}
