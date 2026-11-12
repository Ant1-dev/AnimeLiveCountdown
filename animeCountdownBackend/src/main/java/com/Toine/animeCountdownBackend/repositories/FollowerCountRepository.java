package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.FavoriteMediaEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/** Read-only follower counts per show, from the favmedia table. */
public interface FollowerCountRepository extends org.springframework.data.repository.Repository<FavoriteMediaEntity, Long> {

    @Query("SELECT fm.media.id AS mediaId, COUNT(fm) AS total FROM FavoriteMediaEntity fm GROUP BY fm.media.id")
    List<EpisodeReactionRepository.MediaCount> countFollowersByMedia();

    @Query("SELECT fm.media.id AS mediaId, COUNT(fm) AS total FROM FavoriteMediaEntity fm " +
           "WHERE fm.addedDate >= :since GROUP BY fm.media.id")
    List<EpisodeReactionRepository.MediaCount> countNewFollowersByMedia(@Param("since") Instant since);
}
