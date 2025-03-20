package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.services.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SearchController {
    private final SearchService service;

    public SearchController(SearchService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public ResponseEntity<List<MediaEntity>> search(@RequestParam(required = false) String term) {
        List<MediaEntity> results = service.searchEntities(term);
        return ResponseEntity.ok(results);
    }
}
