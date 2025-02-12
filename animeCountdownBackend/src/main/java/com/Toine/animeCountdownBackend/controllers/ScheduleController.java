package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import com.Toine.animeCountdownBackend.services.ScheduleService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final MediaRepository mediaRepository;

    public ScheduleController(ScheduleService scheduleService, MediaRepository mediaRepository) {
        this.scheduleService = scheduleService;
        this.mediaRepository = mediaRepository;
    }

    @GetMapping("/airing/{weekDay}")
    public List<MediaEntity> getAnimeOnDay(@PathVariable String weekDay) {
        return mediaRepository.findByDayIgnoreCase(weekDay);
    }

    @GetMapping("/airing/trending")
    public List<MediaEntity> getTrending() {
        Pageable pageable = PageRequest.of(0, 5);
        return mediaRepository.findAllBy(pageable).getContent();
    }

    @GetMapping("/airing/soon")
    public List<MediaEntity> getClosestAiring() {
        Pageable pageable = PageRequest.of(0,100);
        return mediaRepository.findAllByOrderByNext_Airing_AtAsc(pageable).getContent();
    }


}
