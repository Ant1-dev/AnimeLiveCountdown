package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.AnimeScheduleLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimeScheduleLinkRepository extends JpaRepository<AnimeScheduleLinkEntity, String> {
}
