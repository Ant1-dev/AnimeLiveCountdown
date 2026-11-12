package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.discovery.LiveResponse;
import com.Toine.animeCountdownBackend.services.discovery.LiveService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/live")
public class LiveController {
    private final LiveService liveService;

    public LiveController(LiveService liveService) {
        this.liveService = liveService;
    }

    @GetMapping("")
    public ResponseEntity<LiveResponse> live() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
                .body(liveService.getLive());
    }
}
