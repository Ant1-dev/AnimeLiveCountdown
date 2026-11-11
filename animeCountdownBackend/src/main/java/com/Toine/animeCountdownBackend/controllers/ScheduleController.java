package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import com.Toine.animeCountdownBackend.services.AiringDayWindows;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/airing")
@Validated
public class ScheduleController {
    private final MediaRepository mediaRepository;

    @Value("${app.media.buffer-days:14}")
    private int bufferDays;

    public ScheduleController(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    @GetMapping("/{weekDay}")
    public ResponseEntity<List<MediaEntity>> getAnimeOnDay(
            @PathVariable
            @Pattern(regexp = "^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)$",
                     message = "Invalid day of week. Must be MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, or SUNDAY")
            String weekDay,
            // Viewer's timezone, e.g. "America/New_York". Invalid or missing values fall back to UTC.
            @RequestParam(name = "tz", required = false)
            @Size(max = 64, message = "Timezone is too long")
            String timeZone) {
        Pageable pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "popularity"));
        Instant now = Instant.now();
        Instant bufferEndDate = now.plus(bufferDays, ChronoUnit.DAYS);
        ZoneId zone = AiringDayWindows.resolveZone(timeZone);
        // Start a week back so rows whose airing time just passed (until the next sync) still show up.
        List<AiringDayWindows.Window> windows = AiringDayWindows.forDay(
                DayOfWeek.valueOf(weekDay), zone, now.minus(7, ChronoUnit.DAYS), bufferEndDate);
        List<MediaEntity> results = mediaRepository.findAll(airingInWindows(windows, bufferEndDate), pageable).getContent();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(results);
    }

    @GetMapping("/soon")
    public ResponseEntity<List<MediaEntity>> getClosestAiring() {
        Pageable pageable = PageRequest.of(0,15);
        List<MediaEntity> results = mediaRepository.findAllByOrderByNext_Airing_AtAsc(pageable).getContent();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(results);
    }

    @GetMapping("/trending")
    public ResponseEntity<List<MediaEntity>> getTrending() {
        Pageable pageable = PageRequest.of(0, 10);
        Instant bufferEndDate = Instant.now().plus(bufferDays, ChronoUnit.DAYS);
        List<MediaEntity> results = mediaRepository.findTrendingByPopularity(bufferEndDate, pageable).getContent();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(results);
    }

    /** Releasing/upcoming shows whose next episode airs before the buffer end and inside one of the windows. */
    private static Specification<MediaEntity> airingInWindows(List<AiringDayWindows.Window> windows, Instant bufferEndDate) {
        return (root, query, cb) -> {
            var airingAt = root.<Instant>get("next_Airing_At");
            Predicate[] inWindow = windows.stream()
                    .map(w -> cb.and(cb.greaterThanOrEqualTo(airingAt, w.start()), cb.lessThan(airingAt, w.end())))
                    .toArray(Predicate[]::new);
            return cb.and(
                    root.get("status").in("RELEASING", "NOT_YET_RELEASED"),
                    cb.isNotNull(airingAt),
                    cb.lessThanOrEqualTo(airingAt, bufferEndDate),
                    cb.or(inWindow));
        };
    }
}
