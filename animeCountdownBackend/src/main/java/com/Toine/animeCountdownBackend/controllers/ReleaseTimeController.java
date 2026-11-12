package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.repositories.ReleaseTimeRepository;
import com.Toine.animeCountdownBackend.services.releases.AnimeScheduleClient;
import com.Toine.animeCountdownBackend.services.releases.ReleaseTimes;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/** When a show's next episode is out subbed and dubbed (streaming platforms included). */
@RestController
@RequestMapping("/api/releases")
public class ReleaseTimeController {
    private final ReleaseTimeRepository releases;
    private final AnimeScheduleClient client;

    public ReleaseTimeController(ReleaseTimeRepository releases, AnimeScheduleClient client) {
        this.releases = releases;
        this.client = client;
    }

    /** enabled=false means the feature isn't configured (no ANIMESCHEDULE_TOKEN); the site hides it. */
    public record ReleasesView(boolean enabled, Map<String, ReleaseTimes.Release> releases) {
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<ReleasesView> releases(@PathVariable Long mediaId) {
        Instant now = Instant.now();
        var rows = releases.findRecentAndUpcoming(mediaId, now.minus(Duration.ofDays(7)));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(new ReleasesView(client.isEnabled(), ReleaseTimes.pick(rows, now)));
    }
}
