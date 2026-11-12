package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Read-only season queries over media_info (kept apart from MediaInfoRepository). */
public interface SeasonMediaInfoRepository extends Repository<MediaInfoEntity, Long> {

    @Query("SELECT DISTINCT m FROM MediaInfoEntity m LEFT JOIN FETCH m.genres " +
           "WHERE m.season = :season AND m.seasonYear = :year")
    List<MediaInfoEntity> findBySeason(@Param("season") String season, @Param("year") int year);
}
