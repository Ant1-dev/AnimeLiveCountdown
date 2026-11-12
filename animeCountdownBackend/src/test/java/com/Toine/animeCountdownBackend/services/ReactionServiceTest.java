package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.dto.ReactionSummary;
import com.Toine.animeCountdownBackend.models.ReactionType;
import com.Toine.animeCountdownBackend.models.postgreEntities.EpisodeReactionEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.EpisodeReactionRepository;
import com.Toine.animeCountdownBackend.repositories.MediaInfoRepository;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reaction rules against the real (local) Postgres schema. Each test runs in a transaction that is
 * rolled back, so nothing is left in the shared dev database.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=update")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReactionServiceTest {

    // Far above any AniList id, so they never collide with synced shows
    private static final long AIRING_SHOW = 990_000_101L;
    private static final long UPCOMING_SHOW = 990_000_102L;
    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

    @Autowired private EpisodeReactionRepository reactionRepository;
    @Autowired private MediaRepository mediaRepository;
    @Autowired private MediaInfoRepository mediaInfoRepository;
    @Autowired private UserRepository userRepository;

    private MutableClock clock;
    private ReactionService service;
    private UserEntity alice;
    private UserEntity bob;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(NOW);
        service = new ReactionService(reactionRepository, mediaRepository, mediaInfoRepository, clock);

        // Episode 5 airs in two days, so episodes 1-4 have aired
        mediaRepository.save(media(AIRING_SHOW, "RELEASING", 5, NOW.plus(Duration.ofDays(2))));
        mediaRepository.save(media(UPCOMING_SHOW, "NOT_YET_RELEASED", 1, NOW.plus(Duration.ofDays(9))));
        alice = userRepository.save(user("alice"));
        bob = userRepository.save(user("bob"));
    }

    @Test
    void reactingCountsPubliclyForTheLatestAiredEpisode() {
        service.react(alice, AIRING_SHOW, 4, "fire");

        ReactionSummary anonymous = service.latestSummary(AIRING_SHOW, null);
        assertEquals(4, anonymous.episode());
        assertEquals(1L, anonymous.counts().get(ReactionType.FIRE));
        assertEquals(1, anonymous.total());
        assertNull(anonymous.mine());

        assertEquals(ReactionType.FIRE, service.latestSummary(AIRING_SHOW, alice).mine());
        assertNull(service.latestSummary(AIRING_SHOW, bob).mine());
    }

    @Test
    void reactingAgainReplacesTheReaction() {
        service.react(alice, AIRING_SHOW, 4, "FIRE");
        ReactionSummary summary = service.react(alice, AIRING_SHOW, 4, "LOVE");

        assertEquals(0L, summary.counts().get(ReactionType.FIRE));
        assertEquals(1L, summary.counts().get(ReactionType.LOVE));
        assertEquals(1, summary.total());
        assertEquals(ReactionType.LOVE, summary.mine());
    }

    @Test
    void sameReactionAgainChangesNothing() {
        service.react(alice, AIRING_SHOW, 4, "LOL");
        Instant firstUpdate = reactionRepository.findByUserAndEpisode(alice.getId(), AIRING_SHOW, 4).orElseThrow().getUpdatedAt();

        clock.advance(Duration.ofHours(1));
        ReactionSummary summary = service.react(alice, AIRING_SHOW, 4, "LOL");

        assertEquals(1, summary.total());
        assertEquals(firstUpdate,
                reactionRepository.findByUserAndEpisode(alice.getId(), AIRING_SHOW, 4).orElseThrow().getUpdatedAt());
    }

    @Test
    void eachUserGetsOneReactionPerEpisode() {
        service.react(alice, AIRING_SHOW, 4, "FIRE");
        service.react(bob, AIRING_SHOW, 4, "FIRE");
        ReactionSummary summary = service.react(bob, AIRING_SHOW, 4, "SAD");

        assertEquals(1L, summary.counts().get(ReactionType.FIRE));
        assertEquals(1L, summary.counts().get(ReactionType.SAD));
        assertEquals(2, summary.total());
    }

    @Test
    void episodesAreCountedSeparately() {
        service.react(alice, AIRING_SHOW, 3, "SHOCK");
        service.react(alice, AIRING_SHOW, 4, "MEH");

        assertEquals(1L, service.summary(AIRING_SHOW, 3, null).counts().get(ReactionType.SHOCK));
        assertEquals(0L, service.summary(AIRING_SHOW, 3, null).counts().get(ReactionType.MEH));
        assertEquals(1L, service.summary(AIRING_SHOW, 4, null).counts().get(ReactionType.MEH));
    }

    @Test
    void databaseRejectsASecondRowForTheSameUserAndEpisode() {
        reactionRepository.saveAndFlush(reaction(alice, 4, ReactionType.FIRE));

        assertThrows(DataIntegrityViolationException.class,
                () -> reactionRepository.saveAndFlush(reaction(alice, 4, ReactionType.LOVE)));
    }

    @Test
    void episodesThatHaventAiredCantBeReactedTo() {
        assertStatus(400, () -> service.react(alice, AIRING_SHOW, 5, "FIRE"));
        assertStatus(400, () -> service.react(alice, AIRING_SHOW, 0, "FIRE"));
        assertStatus(400, () -> service.react(alice, UPCOMING_SHOW, 1, "FIRE"));
    }

    @Test
    void episodeBecomesReactableOnceItAirs() {
        clock.advance(Duration.ofDays(2));

        assertEquals(5, service.react(alice, AIRING_SHOW, 5, "FIRE").episode());
        assertEquals(5, service.latestSummary(AIRING_SHOW, null).episode());
    }

    @Test
    void upcomingShowHasNoLatestEpisode() {
        ReactionSummary summary = service.latestSummary(UPCOMING_SHOW, alice);

        assertNull(summary.episode());
        assertEquals(0, summary.total());
        assertEquals(ReactionType.values().length, summary.counts().size());
    }

    @Test
    void unknownReactionIsRejected() {
        assertStatus(400, () -> service.react(alice, AIRING_SHOW, 4, "THUMBS_UP"));
        assertStatus(400, () -> service.react(alice, AIRING_SHOW, 4, null));
    }

    @Test
    void signedOutUsersCantReact() {
        assertStatus(401, () -> service.react(null, AIRING_SHOW, 4, "FIRE"));
        assertStatus(401, () -> service.remove(null, AIRING_SHOW, 4));
    }

    @Test
    void unknownShowIsNotFound() {
        assertStatus(404, () -> service.latestSummary(990_000_199L, null));
        assertStatus(404, () -> service.react(alice, 990_000_199L, 1, "FIRE"));
    }

    @Test
    void removingTakesTheReactionAway() {
        service.react(alice, AIRING_SHOW, 4, "FIRE");
        service.react(bob, AIRING_SHOW, 4, "FIRE");

        ReactionSummary summary = service.remove(alice, AIRING_SHOW, 4);

        assertEquals(1L, summary.counts().get(ReactionType.FIRE));
        assertNull(summary.mine());
    }

    private static void assertStatus(int status, Executable call) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, call);
        assertEquals(status, e.getStatusCode().value());
    }

    private EpisodeReactionEntity reaction(UserEntity user, int episode, ReactionType type) {
        EpisodeReactionEntity r = new EpisodeReactionEntity();
        r.setUser(user);
        r.setMediaId(AIRING_SHOW);
        r.setEpisode(episode);
        r.setReaction(type);
        r.setCreatedAt(NOW);
        r.setUpdatedAt(NOW);
        return r;
    }

    static MediaEntity media(long id, String status, Integer nextEpisode, Instant nextAiringAt) {
        MediaEntity m = new MediaEntity();
        m.setId(id);
        m.setTitle_Romaji("Test Show " + id);
        m.setStatus(status);
        m.setNext_Airing_Episode(nextEpisode);
        m.setNext_Airing_At(nextAiringAt);
        m.setPopularity(0L);
        return m;
    }

    static UserEntity user(String name) {
        UserEntity u = new UserEntity();
        u.setEmail(name + "-" + UUID.randomUUID() + "@example.test");
        u.setName(name);
        return u;
    }
}
