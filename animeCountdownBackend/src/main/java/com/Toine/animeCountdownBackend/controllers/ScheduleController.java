package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import jakarta.validation.constraints.Pattern;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/airing")
@Validated
public class ScheduleController {
    private final MediaRepository mediaRepository;

    public ScheduleController(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    @Cacheable(value = "weekdayAnime", key = "#weekDay")
    @GetMapping("/{weekDay}")
    public ResponseEntity<List<MediaEntity>> getAnimeOnDay(
            @PathVariable
            @Pattern(regexp = "^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)$",
                     message = "Invalid day of week. Must be MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, or SUNDAY")
            String weekDay) {
        Pageable pageable = PageRequest.of(0, 50);
        List<MediaEntity> results = mediaRepository.findAiringMediaByDayOrderedByPopularity(weekDay, pageable).getContent();
        return ResponseEntity.ok(results);
    }

    @Cacheable(value = "upcomingAnime", key = "'soon'")
    @GetMapping("/soon")
    public List<MediaEntity> getClosestAiring() {
        Pageable pageable = PageRequest.of(0,5);
        return mediaRepository.findAllByOrderByNext_Airing_AtAsc(pageable).getContent();
    }

    @Cacheable(value = "trendingAnime", key = "'currentYear'")
    @GetMapping("/trending")
    public List<MediaEntity> getTrending() {
        Pageable pageable = PageRequest.of(0, 10);
        return mediaRepository.findAllByCurrentYear(pageable).getContent();
    }


}
