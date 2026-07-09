package com.miniopgg.dto;

public record TftSummaryResponse(
        int totalGamesAnalyzed,
        double averagePlacement,
        double topFourRate,
        double firstPlaceRate,
        int bestPlacement,
        String mostPlayedUnit
) {
}
