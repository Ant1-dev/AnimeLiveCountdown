package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import com.Toine.animeCountdownBackend.services.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService service;

    public SearchController(SearchService service) {
        this.service = service;
    }

    @GetMapping("")
    public ResponseEntity<List<MediaInfoEntity>> search(@RequestParam(required = false) String term) {
        List<MediaInfoEntity> results = service.searchEntities(term);


        List<MediaInfoEntity> limitedResults = results.stream()
                .limit(5)
                .collect(Collectors.toList());

        return ResponseEntity.ok(limitedResults);
    }
}
