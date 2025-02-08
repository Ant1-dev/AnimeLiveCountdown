package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<MediaEntity, Long> {
    List<MediaEntity> findByDayIgnoreCase(String day);
}
