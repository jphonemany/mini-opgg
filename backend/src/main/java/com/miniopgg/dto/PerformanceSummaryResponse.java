package com.miniopgg.dto;

public record PerformanceSummaryResponse(
        int totalGamesAnalyzed,
        double recentWinRate,
        double averageKills,
        double averageDeaths,
        double averageAssists,
        double averageKda,
        String mostPlayedChampion
) {
}
