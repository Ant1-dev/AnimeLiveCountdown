package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.graphQlMedia.Page;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import com.Toine.animeCountdownBackend.services.ScheduleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    @GetMapping("/airing")
    public Mono<Page> getCurrentlyAiringAnime() {
        return scheduleService.getCurrentlyAiringAnime();
    }

    @GetMapping("/airing/{weekDay}")
    public List<MediaEntity> getAnimeOnDay(@PathVariable String weekDay) {
        return mediaRepository.findByDayIgnoreCase(weekDay);
    }


}
