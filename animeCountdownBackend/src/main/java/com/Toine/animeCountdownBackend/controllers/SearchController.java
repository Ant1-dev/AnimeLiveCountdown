package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import com.Toine.animeCountdownBackend.services.SearchService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@Validated
public class SearchController {
    private final SearchService service;

    public SearchController(SearchService service) {
        this.service = service;
    }

    @GetMapping("")
    public ResponseEntity<List<MediaInfoEntity>> search(
            @RequestParam(required = false)
            @NotBlank(message = "Search term cannot be empty")
            @Size(min = 1, max = 100, message = "Search term must be between 1 and 100 characters")
            String term) {

        if (term == null || term.trim().isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<MediaInfoEntity> results = service.searchEntities(term);
        return ResponseEntity.ok(results);
    }
}
