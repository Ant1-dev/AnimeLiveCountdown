package com.Toine.animeCountdownBackend.repositories;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Writes delay alerts into the notification bell for all of a show's followers in one statement.
 *
 * <p>The notification unique key (user_id, media_id, episode, kind) makes this the dedupe: an alert
 * is created once per follower, episode and kind (DELAY or DELAY_DUB), however many sync runs or
 * sources (AniList, AnimeSchedule) report the same delay. If the episode is pushed back again by
 * more than {@link #RE_ALERT_AFTER}, the existing row is updated to the new date and marked unread,
 * and the follower is alerted again.
 */
@Repository
public class DelayAlertRepository {

    /** A later date within this of the one already sent is treated as the same delay. */
    public static final Duration RE_ALERT_AFTER = Duration.ofHours(24);

    private static final String UPSERT = """
            INSERT INTO notification_entity
                (id, user_id, media_id, episode, airing_at, notif_date, title, message, image_url, kind, read_at)
            SELECT nextval('notification_entity_seq'), f.user_id, :mediaId, :episode, :newAt, :now,
                   :title, :message, :imageUrl, :kind, NULL
            FROM favmedia f
            LEFT JOIN reminder r ON r.user_id = f.user_id AND r.media_id = f.media_id
            WHERE f.media_id = :mediaId
              AND (f.added_date IS NULL OR f.added_date <= :followedBy)
              AND (COALESCE(r.release_type, 'JAPAN') = 'DUB') = :dubAudience
            ON CONFLICT (user_id, media_id, episode, kind) DO UPDATE
              SET airing_at = EXCLUDED.airing_at,
                  notif_date = EXCLUDED.notif_date,
                  message = EXCLUDED.message,
                  read_at = NULL
              WHERE notification_entity.airing_at IS NULL
                 OR EXCLUDED.airing_at > notification_entity.airing_at + make_interval(secs => :reAlertSeconds)
            RETURNING user_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public DelayAlertRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Records the alert for every follower of the show who followed by {@code followedBy} and is in
     * the audience: dub watchers (a DUB reminder) for dub delays, everyone else otherwise.
     *
     * @return the users who were newly alerted (and should get push/Discord)
     */
    @Transactional
    public List<Long> upsertForFollowers(long mediaId, int episode, String kind, boolean dubAudience,
                                         Instant newAt, Instant followedBy, Instant now,
                                         String title, String message, String imageUrl) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("mediaId", mediaId)
                .addValue("episode", episode)
                .addValue("kind", kind)
                .addValue("dubAudience", dubAudience)
                .addValue("newAt", Timestamp.from(newAt))
                .addValue("followedBy", Timestamp.from(followedBy))
                .addValue("now", Timestamp.from(now))
                .addValue("title", title)
                .addValue("message", message)
                .addValue("imageUrl", imageUrl)
                .addValue("reAlertSeconds", (double) RE_ALERT_AFTER.toSeconds());
        return jdbc.queryForList(UPSERT, params, Long.class);
    }
}
