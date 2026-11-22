package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<MediaEntity, Long>, JpaSpecificationExecutor<MediaEntity> {

    @Query("SELECT m FROM MediaEntity m WHERE m.status IN ('RELEASING', 'NOT_YET_RELEASED') AND m.next_Airing_At IS NOT NULL AND m.next_Airing_At <= :bufferEndDate ORDER BY m.popularity DESC")
    Page<MediaEntity> findTrendingByPopularity(@Param("bufferEndDate") Instant bufferEndDate, Pageable pageable);

    /** Next episodes that haven't aired yet, soonest first. */
    @Query("SELECT m FROM MediaEntity m WHERE m.next_Airing_At > :now AND m.status IN ('RELEASING', 'NOT_YET_RELEASED') ORDER BY m.next_Airing_At ASC")
    Page<MediaEntity> findUpcomingEpisodes(@Param("now") Instant now, Pageable pageable);

    @Query("SELECT m.id FROM MediaEntity m")
    List<Long> findAllIds();

    /** Shows the upcoming sweep is responsible for. */
    @Query("SELECT m FROM MediaEntity m WHERE m.status = 'NOT_YET_RELEASED'")
    List<MediaEntity> findUpcoming();

    /** Shows the airing sweep is responsible for: everything that isn't upcoming. */
    @Query("SELECT m FROM MediaEntity m WHERE m.status IS NULL OR m.status <> 'NOT_YET_RELEASED'")
    List<MediaEntity> findNotUpcoming();

    /** Shows whose stored next episode time falls in (from, to], i.e. episodes that have just aired. */
    @Query("SELECT m.id FROM MediaEntity m WHERE m.next_Airing_At > :from AND m.next_Airing_At <= :to")
    List<Long> findIdsAiringBetween(@Param("from") Instant from, @Param("to") Instant to);


    /** Shows whose break/delay note DelayAlertService hasn't told followers about yet. */
    @Query("SELECT m FROM MediaEntity m WHERE m.scheduleNoteAlertPending = true")
    List<MediaEntity> findPendingDelayAlerts();

    /**
     * Marks the alert sent, unless the sync moved the note's date in the meantime (then the next run
     * picks up the new date).
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("""
            UPDATE MediaEntity m SET m.scheduleNoteAlertPending = false
            WHERE m.id = :id AND (m.scheduleNoteExpiresAt IS NULL OR m.scheduleNoteExpiresAt = :expiresAt)
            """)
    int clearDelayAlertPending(@Param("id") Long id, @Param("expiresAt") Instant expiresAt);
}
