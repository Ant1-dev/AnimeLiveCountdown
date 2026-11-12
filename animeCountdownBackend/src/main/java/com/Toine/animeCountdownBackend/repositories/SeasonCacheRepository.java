package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.SeasonCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonCacheRepository extends JpaRepository<SeasonCacheEntity, String> {
}
