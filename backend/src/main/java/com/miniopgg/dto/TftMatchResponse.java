package com.miniopgg.dto;

import java.time.Instant;
import java.util.List;

public record TftMatchResponse(
        String matchId,
        int placement,
        int level,
        long gameLength,
        int queueId,
        String gameType,
        Instant playedAt,
        List<String> units,
        List<String> traits
) {
}
