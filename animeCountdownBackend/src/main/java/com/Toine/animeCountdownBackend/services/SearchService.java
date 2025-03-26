package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import com.Toine.animeCountdownBackend.repositories.MediaInfoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {
    private final MediaInfoRepository repository;

    public SearchService(MediaInfoRepository repository) {
        this.repository = repository;
    }

    public List<MediaInfoEntity> searchEntities(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return repository.searchByFields(searchTerm.trim());
    }


}
