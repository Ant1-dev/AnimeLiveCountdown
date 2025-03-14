package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {
    private final MediaRepository repository;

    public SearchService(MediaRepository repository) {
        this.repository = repository;
    }

    public List<MediaEntity> searchEntities(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return repository.searchByFields(searchTerm.trim());
    }


}
