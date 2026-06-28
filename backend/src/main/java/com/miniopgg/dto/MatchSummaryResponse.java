package com.miniopgg.dto;

import java.time.Instant;

public record MatchSummaryResponse(
        String matchId,
        String championName,
        int kills,
        int deaths,
        int assists,
        boolean win,
        int cs,
        long gameDuration,
        String gameMode,
        Instant playedAt
) {
}
