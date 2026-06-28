package com.miniopgg.controller;

import com.miniopgg.dto.MatchSummaryResponse;
import com.miniopgg.dto.PerformanceSummaryResponse;
import com.miniopgg.dto.PlayerProfileResponse;
import com.miniopgg.dto.RankedInfoResponse;
import com.miniopgg.service.PlayerAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/player")
public class PlayerController {
    private final PlayerAnalyticsService playerAnalyticsService;

    public PlayerController(PlayerAnalyticsService playerAnalyticsService) {
        this.playerAnalyticsService = playerAnalyticsService;
    }

    @GetMapping("/{gameName}/{tagLine}")
    public PlayerProfileResponse getPlayer(@PathVariable String gameName, @PathVariable String tagLine) {
        return playerAnalyticsService.getProfile(gameName, tagLine);
    }

    @GetMapping("/{gameName}/{tagLine}/rank")
    public RankedInfoResponse getRank(@PathVariable String gameName, @PathVariable String tagLine) {
        return playerAnalyticsService.getRank(gameName, tagLine);
    }

    @GetMapping("/{gameName}/{tagLine}/matches")
    public List<MatchSummaryResponse> getMatches(@PathVariable String gameName, @PathVariable String tagLine) {
        return playerAnalyticsService.getMatches(gameName, tagLine);
    }

    @GetMapping("/{gameName}/{tagLine}/summary")
    public PerformanceSummaryResponse getSummary(@PathVariable String gameName, @PathVariable String tagLine) {
        return playerAnalyticsService.getSummary(gameName, tagLine);
    }
}
