package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaInfoRepository extends JpaRepository<MediaInfoEntity, Long> {
    @Query("SELECT m.id FROM MediaEntity m")
    List<Long> findAllIds();
}
