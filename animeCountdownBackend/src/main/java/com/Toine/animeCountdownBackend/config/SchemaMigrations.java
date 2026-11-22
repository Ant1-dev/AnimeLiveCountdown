package com.Toine.animeCountdownBackend.config;

import com.Toine.animeCountdownBackend.models.postgreEntities.NotificationEntity;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Schema changes that {@code ddl-auto=update} can't make by itself, run once at startup after
 * Hibernate has updated the schema. Every statement is idempotent, so it is safe on a fresh
 * database, on one that already ran it, and on every restart.
 *
 * <p>Hibernate adds new columns and constraints but never drops or replaces an existing one. The
 * notification dedupe key changed from (user_id, media_id, episode) to (user_id, media_id, episode,
 * kind) so one user can get a sub and a dub reminder (and a delay alert) for the same episode; the
 * old, stricter constraint has to be dropped here or those inserts would fail.
 */
@Component
public class SchemaMigrations {
    private static final Logger logger = LoggerFactory.getLogger(SchemaMigrations.class);

    static final List<String> STATEMENTS = List.of(
            // Column fallbacks in case Hibernate's ALTER failed; no-ops when the columns exist.
            "ALTER TABLE reminder ADD COLUMN IF NOT EXISTS release_type varchar(8) NOT NULL DEFAULT 'JAPAN'",
            "ALTER TABLE media ADD COLUMN IF NOT EXISTS schedule_note_alert_pending boolean NOT NULL DEFAULT false",
            "ALTER TABLE release_time ADD COLUMN IF NOT EXISTS delay_alert_pending boolean NOT NULL DEFAULT false",
            "ALTER TABLE release_time ADD COLUMN IF NOT EXISTS delay_changed_at timestamp(6) with time zone",
            "ALTER TABLE notification_entity ADD COLUMN IF NOT EXISTS link varchar(1024)",
            // Drop the old (user_id, media_id, episode) unique constraint, whatever it is named.
            """
            DO $$
            DECLARE c record;
            BEGIN
              FOR c IN
                SELECT con.conname
                FROM pg_constraint con
                WHERE con.conrelid = 'notification_entity'::regclass
                  AND con.contype = 'u'
                  AND (SELECT array_agg(a.attname::text ORDER BY a.attname::text)
                       FROM unnest(con.conkey) AS k(attnum)
                       JOIN pg_attribute a ON a.attrelid = con.conrelid AND a.attnum = k.attnum)
                      = ARRAY['episode', 'media_id', 'user_id']
              LOOP
                EXECUTE format('ALTER TABLE notification_entity DROP CONSTRAINT %I', c.conname);
              END LOOP;
            END $$
            """,
            // Add the new key unless Hibernate already did.
            """
            DO $$
            BEGIN
              IF NOT EXISTS (SELECT 1 FROM pg_constraint
                             WHERE conrelid = 'notification_entity'::regclass
                               AND conname = '%s') THEN
                ALTER TABLE notification_entity
                  ADD CONSTRAINT %s UNIQUE (user_id, media_id, episode, kind);
              END IF;
            END $$
            """.formatted(NotificationEntity.UNIQUE_KEY, NotificationEntity.UNIQUE_KEY));

    private final JdbcTemplate jdbc;

    /** Takes the EntityManagerFactory only so Hibernate's schema update has run first. */
    public SchemaMigrations(JdbcTemplate jdbc, EntityManagerFactory entityManagerFactory) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void migrate() {
        for (String sql : STATEMENTS) {
            try {
                jdbc.execute(sql);
            } catch (RuntimeException e) {
                // Don't block startup; reminders still work, only sub+dub for one episode would clash
                logger.error("Schema migration step failed: {}", sql.strip().lines().findFirst().orElse(sql), e);
            }
        }
    }
}
