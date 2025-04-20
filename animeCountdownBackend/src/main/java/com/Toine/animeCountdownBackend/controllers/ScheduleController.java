package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/airing")
public class ScheduleController {
    private final MediaRepository mediaRepository;

    public ScheduleController(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    @GetMapping("/{weekDay}")
    public List<MediaEntity> getAnimeOnDay(@PathVariable String weekDay) {
        return mediaRepository.findAiringMediaByDayOrderedByPopularity(weekDay);
    }

    @GetMapping("/soon")
    public List<MediaEntity> getClosestAiring() {
        Pageable pageable = PageRequest.of(0,5);
        return mediaRepository.findAllByOrderByNext_Airing_AtAsc(pageable).getContent();
    }

    @GetMapping("/trending")
    public List<MediaEntity> getTrending() {
        Pageable pageable = PageRequest.of(0, 10);
        return mediaRepository.findAllByCurrentYear(pageable).getContent();
    }


}
