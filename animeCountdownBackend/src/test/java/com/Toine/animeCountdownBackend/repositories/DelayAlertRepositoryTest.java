package com.Toine.animeCountdownBackend.repositories;

import com.Toine.animeCountdownBackend.config.SchemaMigrations;
import com.Toine.animeCountdownBackend.models.postgreEntities.FavoriteMediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.NotificationEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.ReminderEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Delay alert upserts and the (user, media, episode, kind) dedupe key against the real (local)
 * Postgres schema, after SchemaMigrations has replaced the old key. Rolled back after each test.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=update")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({DelayAlertRepository.class, SchemaMigrations.class})
class DelayAlertRepositoryTest {

    private static final long SHOW = 990_000_301L;
    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    private static final Instant DETECTED = NOW.minus(Duration.ofMinutes(5));
    private static final Instant NEW_AT = Instant.parse("2026-12-06T15:00:00Z");

    @Autowired private DelayAlertRepository delayAlerts;
    @Autowired private NotificationRepository notifications;
    @Autowired private ReminderRepository reminders;
    @Autowired private FavoriteMediaRepository favorites;
    @Autowired private MediaRepository mediaRepository;
    @Autowired private UserRepository users;
    @Autowired private JdbcTemplate jdbc;

    private UserEntity follower, dubWatcher, lateFollower;

    @BeforeEach
    void setUp() {
        MediaEntity show = mediaRepository.save(media(SHOW, "RELEASING", 7, NEW_AT));
        follower = users.save(user("follower"));
        dubWatcher = users.save(user("dub"));
        lateFollower = users.save(user("late"));
        follow(follower, show, DETECTED.minus(Duration.ofDays(10)));
        follow(dubWatcher, show, DETECTED.minus(Duration.ofDays(10)));
        follow(lateFollower, show, DETECTED.plus(Duration.ofMinutes(1)));
        ReminderEntity dub = new ReminderEntity();
        dub.setUser(dubWatcher);
        dub.setMediaId(SHOW);
        dub.setReleaseType(ReminderEntity.TYPE_DUB);
        reminders.save(dub);
        reminders.flush();
    }

    private static MediaEntity media(long id, String status, Integer nextEpisode, Instant nextAiringAt) {
        MediaEntity m = new MediaEntity();
        m.setId(id);
        m.setTitle_Romaji("Test Show " + id);
        m.setStatus(status);
        m.setNext_Airing_Episode(nextEpisode);
        m.setNext_Airing_At(nextAiringAt);
        m.setPopularity(0L);
        return m;
    }

    private static UserEntity user(String name) {
        UserEntity u = new UserEntity();
        u.setEmail(name + "-" + java.util.UUID.randomUUID() + "@example.test");
        u.setName(name);
        return u;
    }

    private void follow(UserEntity u, MediaEntity m, Instant at) {
        FavoriteMediaEntity f = new FavoriteMediaEntity();
        f.setUser(u);
        f.setMedia(m);
        f.setAddedDate(at);
        favorites.saveAndFlush(f);
    }

    private List<Long> upsert(String kind, boolean dub, Instant newAt) {
        return delayAlerts.upsertForFollowers(SHOW, 7, kind, dub, newAt, DETECTED, NOW, "Show", "Episode 7 delayed", null);
    }

    @Test
    void oldThreeColumnKeyIsReplacedByOneWithKind() {
        List<String> keys = jdbc.queryForList("""
                SELECT pg_get_constraintdef(oid) FROM pg_constraint
                WHERE conrelid = 'notification_entity'::regclass AND contype = 'u'
                """, String.class);
        assertTrue(keys.contains("UNIQUE (user_id, media_id, episode, kind)"), keys.toString());
        assertFalse(keys.contains("UNIQUE (user_id, media_id, episode)"), keys.toString());
        assertEquals("JAPAN", jdbc.queryForObject(
                "SELECT column_default FROM information_schema.columns WHERE table_name = 'reminder' AND column_name = 'release_type'",
                String.class).replaceAll("'|::.*", ""));
    }

    @Test
    void sameEpisodeCanBeNotifiedOncePerKind() {
        Long u = follower.getId();
        assertEquals(1, notifications.insertEpisodeIfAbsent(u, SHOW, 7, NEW_AT, NOW, "Show", "Japan", null, "EPISODE", null));
        assertEquals(1, notifications.insertEpisodeIfAbsent(u, SHOW, 7, NEW_AT, NOW, "Show", "Sub", null, "EPISODE_SUB", null));
        assertEquals(1, notifications.insertEpisodeIfAbsent(u, SHOW, 7, NEW_AT, NOW, "Show", "Dub", null, "EPISODE_DUB", "https://x"));
        assertEquals(0, notifications.insertEpisodeIfAbsent(u, SHOW, 7, NEW_AT, NOW, "Show", "Dub", null, "EPISODE_DUB", null));
        assertEquals(List.of(u), upsert(NotificationEntity.KIND_DELAY, false, NEW_AT));
    }

    @Test
    void delayGoesToFollowersFromBeforeItWasSeenSplitByAudience() {
        assertEquals(List.of(follower.getId()), upsert(NotificationEntity.KIND_DELAY, false, NEW_AT));
        assertEquals(List.of(dubWatcher.getId()), upsert(NotificationEntity.KIND_DELAY_DUB, true, NEW_AT));
    }

    @Test
    void sameDelayFromAnotherRunOrSourceIsNotSentAgain() {
        upsert(NotificationEntity.KIND_DELAY, false, NEW_AT);

        assertEquals(List.of(), upsert(NotificationEntity.KIND_DELAY, false, NEW_AT));
        // AnimeSchedule's sub time for the same delay, an hour after the Japanese airing
        assertEquals(List.of(), upsert(NotificationEntity.KIND_DELAY, false, NEW_AT.plus(Duration.ofHours(1))));
    }

    @Test
    void delayPushedBackFurtherAlertsAgainWithTheNewDate() {
        upsert(NotificationEntity.KIND_DELAY, false, NEW_AT);
        notifications.markAllRead(follower.getId(), NOW);
        Instant later = NEW_AT.plus(Duration.ofDays(7));

        assertEquals(List.of(follower.getId()), upsert(NotificationEntity.KIND_DELAY, false, later));

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT airing_at, read_at FROM notification_entity WHERE user_id = ? AND media_id = ? AND kind = 'DELAY'",
                follower.getId(), SHOW);
        assertEquals(later, ((java.sql.Timestamp) row.get("airing_at")).toInstant());
        assertNull(row.get("read_at"));
    }
}
