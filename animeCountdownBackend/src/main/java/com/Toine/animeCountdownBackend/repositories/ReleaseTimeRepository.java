package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.ReleaseTimeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReleaseTimeRepository extends JpaRepository<ReleaseTimeEntity, Long> {

    Optional<ReleaseTimeEntity> findByMediaIdAndAirTypeAndEpisode(Long mediaId, String airType, Integer episode);

    @Query("SELECT r FROM ReleaseTimeEntity r WHERE r.mediaId = :mediaId AND r.releaseAt >= :since ORDER BY r.releaseAt")
    List<ReleaseTimeEntity> findRecentAndUpcoming(@Param("mediaId") Long mediaId, @Param("since") Instant since);

    /** Media id and streams JSON of each recent or upcoming release, for "which services carry this show". */
    @Query("SELECT r.mediaId, r.streamsJson FROM ReleaseTimeEntity r WHERE r.mediaId IN :mediaIds AND r.releaseAt >= :since")
    List<Object[]> findStreamsByMediaIds(@Param("mediaIds") Collection<Long> mediaIds, @Param("since") Instant since);

    @Modifying
    @Transactional
    @Query("DELETE FROM ReleaseTimeEntity r WHERE r.releaseAt < :before")
    int deleteOlderThan(@Param("before") Instant before);
}
