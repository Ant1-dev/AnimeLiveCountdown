package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.services.notifications.CalendarFeedService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** Public calendar feed. The secret token in the URL is the only credential. */
@RestController
public class CalendarFeedController {
    private static final MediaType TEXT_CALENDAR = new MediaType("text", "calendar", StandardCharsets.UTF_8);

    private final CalendarFeedService calendarFeedService;

    public CalendarFeedController(CalendarFeedService calendarFeedService) {
        this.calendarFeedService = calendarFeedService;
    }

    @GetMapping("/api/calendar/{token}.ics")
    public ResponseEntity<String> feed(@PathVariable String token) {
        return calendarFeedService.feedForToken(token, Instant.now())
                .map(ics -> ResponseEntity.ok()
                        .contentType(TEXT_CALENDAR)
                        .cacheControl(CacheControl.noStore())
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"anime-live-countdown.ics\"")
                        .header("X-Robots-Tag", "noindex")
                        .body(ics))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
