package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.dto.HypeEntry;
import com.Toine.animeCountdownBackend.services.HypeService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** "Most hyped this week" for the home page. */
@RestController
public class HypeController {

    private final HypeService hypeService;

    public HypeController(HypeService hypeService) {
        this.hypeService = hypeService;
    }

    @GetMapping("/api/hype")
    public ResponseEntity<List<HypeEntry>> mostHyped(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(hypeService.mostHyped(limit));
    }
}
