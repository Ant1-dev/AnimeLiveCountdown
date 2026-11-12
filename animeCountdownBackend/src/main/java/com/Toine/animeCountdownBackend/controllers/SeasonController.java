package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.discovery.SeasonKey;
import com.Toine.animeCountdownBackend.models.discovery.SeasonOverview;
import com.Toine.animeCountdownBackend.models.discovery.SeasonResponse;
import com.Toine.animeCountdownBackend.models.discovery.SeasonSort;
import com.Toine.animeCountdownBackend.services.discovery.SeasonService;
import com.Toine.animeCountdownBackend.services.discovery.SeasonUnavailableException;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Season browser: /api/search/season?season=FALL&year=2026&genre=Action&sort=score */
@RestController
@RequestMapping("/api/search/season")
@Validated
public class SeasonController {
    private final SeasonService seasonService;

    public SeasonController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @GetMapping("/overview")
    public ResponseEntity<SeasonOverview> overview() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .body(seasonService.overview());
    }

    @GetMapping("")
    public ResponseEntity<?> season(
            @RequestParam(required = false) @Size(max = 10) String season,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @Size(max = 40) String genre,
            @RequestParam(required = false) @Size(max = 20) String sort) {
        SeasonKey key;
        SeasonSort seasonSort;
        try {
            SeasonKey current = seasonService.currentSeason();
            key = season == null && year == null ? current
                    : SeasonKey.parse(season == null ? current.season() : season, year == null ? current.year() : year);
            seasonSort = SeasonSort.parse(sort);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        try {
            SeasonResponse response = seasonService.getSeason(key, genre, seasonSort);
            // Past seasons barely change; current/upcoming ones follow the media_info sync.
            CacheControl cache = "anilist".equals(response.source())
                    ? CacheControl.maxAge(6, TimeUnit.HOURS) : CacheControl.maxAge(5, TimeUnit.MINUTES);
            return ResponseEntity.ok().cacheControl(cache).body(response);
        } catch (SeasonUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header("Retry-After", "120")
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
