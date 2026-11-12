package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.models.postgreEntities.NotificationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    @Query("SELECT n FROM NotificationEntity n WHERE n.user.id = :userId ORDER BY n.notifDate DESC, n.id DESC")
    List<NotificationEntity> findRecent(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.user.id = :userId AND n.readAt IS NULL")
    long countUnread(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE NotificationEntity n SET n.readAt = :now WHERE n.user.id = :userId AND n.id = :id AND n.readAt IS NULL")
    int markRead(@Param("userId") Long userId, @Param("id") Long id, @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("UPDATE NotificationEntity n SET n.readAt = :now WHERE n.user.id = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("userId") Long userId, @Param("now") Instant now);

    /**
     * Records an episode notification unless this user was already notified about this episode.
     * Returns 1 if the row was inserted (the caller should deliver it) and 0 if it already existed.
     * Atomic in the database, so concurrent job runs or several backend instances can't double-send.
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO notification_entity
                (id, user_id, media_id, episode, airing_at, notif_date, title, message, image_url, kind)
            VALUES (nextval('notification_entity_seq'), :userId, :mediaId, :episode, :airingAt, :now,
                    :title, :message, :imageUrl, 'EPISODE')
            ON CONFLICT (user_id, media_id, episode) DO NOTHING
            """, nativeQuery = true)
    int insertEpisodeIfAbsent(@Param("userId") Long userId,
                              @Param("mediaId") Long mediaId,
                              @Param("episode") Integer episode,
                              @Param("airingAt") Instant airingAt,
                              @Param("now") Instant now,
                              @Param("title") String title,
                              @Param("message") String message,
                              @Param("imageUrl") String imageUrl);

    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationEntity n WHERE n.notifDate < :before")
    int deleteOlderThan(@Param("before") Instant before);
}
