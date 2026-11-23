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

    @Query("SELECT r FROM ReleaseTimeEntity r WHERE r.delayAlertPending = true")
    List<ReleaseTimeEntity> findPendingDelayAlerts();

    /** Marks the alert sent, unless the sync changed delayed_until in the meantime. */
    @Modifying
    @Transactional
    @Query("""
            UPDATE ReleaseTimeEntity r SET r.delayAlertPending = false
            WHERE r.id = :id AND (r.delayedUntil IS NULL OR r.delayedUntil = :delayedUntil)
            """)
    int clearDelayAlertPending(@Param("id") Long id, @Param("delayedUntil") Instant delayedUntil);

    /** The next release of one type for a show, counting a delayed one at its delayed_until. */
    @Query("""
            SELECT r FROM ReleaseTimeEntity r
            WHERE r.mediaId = :mediaId AND r.airType = :airType
              AND (r.releaseAt >= :since OR r.delayedUntil >= :since)
            ORDER BY r.releaseAt
            """)
    List<ReleaseTimeEntity> findUpcoming(@Param("mediaId") Long mediaId, @Param("airType") String airType,
                                         @Param("since") Instant since);

    /** Which air types (SUB, DUB, ...) have any release rows for each of these shows. */
    @Query("SELECT DISTINCT r.mediaId, r.airType FROM ReleaseTimeEntity r WHERE r.mediaId IN :mediaIds")
    List<Object[]> findAirTypes(@Param("mediaIds") java.util.Collection<Long> mediaIds);
}
