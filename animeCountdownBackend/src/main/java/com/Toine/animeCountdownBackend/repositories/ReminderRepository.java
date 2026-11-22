package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.ReminderEntity;
import com.Toine.animeCountdownBackend.services.notifications.ReminderCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends JpaRepository<ReminderEntity, Long> {

    List<ReminderEntity> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<ReminderEntity> findByUser_IdAndMediaId(Long userId, Long mediaId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ReminderEntity r WHERE r.user.id = :userId AND r.mediaId = :mediaId")
    int deleteByUserAndMedia(@Param("userId") Long userId, @Param("mediaId") Long mediaId);

    /**
     * JAPAN reminders on shows the user still follows whose next episode airs in (from, to].
     * DueEpisodeSelector then decides which of them are due right now.
     */
    @Query("""
            SELECT new com.Toine.animeCountdownBackend.services.notifications.ReminderCandidate(
                r.user.id, r.mediaId, r.leadMinutes, r.pushEnabled, r.discordEnabled,
                m.next_Airing_At, m.next_Airing_Episode, m.title_English, m.title_Romaji, m.cover_Image_Url)
            FROM ReminderEntity r JOIN MediaEntity m ON m.id = r.mediaId
            WHERE r.releaseType = 'JAPAN'
              AND m.next_Airing_At > :from AND m.next_Airing_At <= :to
              AND m.next_Airing_Episode IS NOT NULL
              AND EXISTS (SELECT 1 FROM FavoriteMediaEntity f WHERE f.user.id = r.user.id AND f.media.id = r.mediaId)
            """)
    List<ReminderCandidate> findCandidates(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * SUB/DUB reminders on shows the user still follows, joined with each release of that type that
     * is out in (from, to], counting a delayed release at its delayed_until.
     */
    @Query("""
            SELECT new com.Toine.animeCountdownBackend.services.notifications.ReminderCandidate(
                r.user.id, r.mediaId, r.leadMinutes, r.pushEnabled, r.discordEnabled,
                rt.releaseAt, rt.delayedUntil, rt.episode, m.title_English, m.title_Romaji, m.cover_Image_Url,
                r.releaseType, rt.streamsJson)
            FROM ReminderEntity r
              JOIN ReleaseTimeEntity rt ON rt.mediaId = r.mediaId AND rt.airType = r.releaseType
              JOIN MediaEntity m ON m.id = r.mediaId
            WHERE r.releaseType IN ('SUB', 'DUB')
              AND ((rt.releaseAt > :from AND rt.releaseAt <= :to)
                   OR (rt.delayedUntil > :from AND rt.delayedUntil <= :to))
              AND EXISTS (SELECT 1 FROM FavoriteMediaEntity f WHERE f.user.id = r.user.id AND f.media.id = r.mediaId)
            """)
    List<ReminderCandidate> findReleaseCandidates(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT new com.Toine.animeCountdownBackend.services.notifications.ReminderChannels(
                r.user.id, r.pushEnabled, r.discordEnabled)
            FROM ReminderEntity r WHERE r.mediaId = :mediaId AND r.user.id IN :userIds
            """)
    List<com.Toine.animeCountdownBackend.services.notifications.ReminderChannels> findChannels(@Param("mediaId") Long mediaId, @Param("userIds") java.util.Collection<Long> userIds);
}
