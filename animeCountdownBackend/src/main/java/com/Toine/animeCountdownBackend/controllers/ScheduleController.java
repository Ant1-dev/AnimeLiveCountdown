package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.graphQlMedia.Page;
import com.Toine.animeCountdownBackend.services.ScheduleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/anime")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/currently-airing")
    public Mono<Page> getCurrentlyAiringAnime() {
        return scheduleService.getCurrentlyAiringAnime();
    }

}
