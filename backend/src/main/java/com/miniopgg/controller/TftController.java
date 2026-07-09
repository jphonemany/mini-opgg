package com.miniopgg.controller;

import com.miniopgg.dto.TftMatchResponse;
import com.miniopgg.dto.TftProfileResponse;
import com.miniopgg.dto.TftRankResponse;
import com.miniopgg.dto.TftSummaryResponse;
import com.miniopgg.service.TftAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tft/player")
public class TftController {
    private final TftAnalyticsService tftAnalyticsService;

    public TftController(TftAnalyticsService tftAnalyticsService) {
        this.tftAnalyticsService = tftAnalyticsService;
    }

    @GetMapping("/{gameName}/{tagLine}")
    public TftProfileResponse getPlayer(@PathVariable String gameName, @PathVariable String tagLine) {
        return tftAnalyticsService.getProfile(gameName, tagLine);
    }

    @GetMapping("/{gameName}/{tagLine}/rank")
    public TftRankResponse getRank(@PathVariable String gameName, @PathVariable String tagLine) {
        return tftAnalyticsService.getRank(gameName, tagLine);
    }

    @GetMapping("/{gameName}/{tagLine}/matches")
    public List<TftMatchResponse> getMatches(@PathVariable String gameName, @PathVariable String tagLine) {
        return tftAnalyticsService.getMatches(gameName, tagLine);
    }

    @GetMapping("/{gameName}/{tagLine}/summary")
    public TftSummaryResponse getSummary(@PathVariable String gameName, @PathVariable String tagLine) {
        return tftAnalyticsService.getSummary(gameName, tagLine);
    }
}
