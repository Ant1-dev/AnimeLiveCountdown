package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.dto.HypeEntry;
import com.Toine.animeCountdownBackend.models.ReactionType;
import com.Toine.animeCountdownBackend.models.postgreEntities.EpisodeReactionEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.FavoriteMediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.EpisodeReactionRepository;
import com.Toine.animeCountdownBackend.repositories.FavoriteMediaRepository;
import com.Toine.animeCountdownBackend.repositories.FollowerCountRepository;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static com.Toine.animeCountdownBackend.services.ReactionServiceTest.media;
import static com.Toine.animeCountdownBackend.services.ReactionServiceTest.user;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Most Hyped ranking against the real (local) Postgres schema, rolled back after each test. The
 * shared dev database may hold real favorites, so assertions only look at the test's own shows.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=update")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HypeServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");
    private static final long REACTED = 990_000_201L;    // 2 reactions this week           -> 6
    private static final long FOLLOWED = 990_000_202L;   // 3 old followers, 1 old reaction  -> 3
    private static final long FINISHED = 990_000_203L;   // lots of activity but not airing  -> excluded
    private static final long QUIET = 990_000_204L;      // no activity                      -> excluded
    private static final long NEW_FOLLOW = 990_000_205L; // 1 follower added this week       -> 2 + 1 = 3
    private static final Set<Long> TEST_SHOWS = Set.of(REACTED, FOLLOWED, FINISHED, QUIET, NEW_FOLLOW);

    @Autowired private EpisodeReactionRepository reactionRepository;
    @Autowired private FollowerCountRepository followerCountRepository;
    @Autowired private FavoriteMediaRepository favoriteMediaRepository;
    @Autowired private MediaRepository mediaRepository;
    @Autowired private UserRepository userRepository;

    private HypeService service;
    private UserEntity u1, u2, u3;

    @BeforeEach
    void setUp() {
        service = new HypeService(reactionRepository, followerCountRepository, mediaRepository, new MutableClock(NOW));
        u1 = userRepository.save(user("u1"));
        u2 = userRepository.save(user("u2"));
        u3 = userRepository.save(user("u3"));

        Instant nextWeek = NOW.plus(Duration.ofDays(7));
        mediaRepository.save(media(REACTED, "RELEASING", 6, nextWeek));
        MediaEntity followed = mediaRepository.save(media(FOLLOWED, "RELEASING", 6, nextWeek));
        MediaEntity finished = mediaRepository.save(media(FINISHED, "FINISHED", null, null));
        mediaRepository.save(media(QUIET, "RELEASING", 6, nextWeek));
        MediaEntity newFollow = media(NEW_FOLLOW, "RELEASING", 6, nextWeek);
        newFollow.setPopularity(5_000L); // ties with FOLLOWED on score, wins on popularity
        mediaRepository.save(newFollow);

        Instant thisWeek = NOW.minus(Duration.ofDays(2));
        Instant lastMonth = NOW.minus(Duration.ofDays(30));

        reaction(u1, REACTED, 5, thisWeek);
        reaction(u2, REACTED, 4, thisWeek);
        reaction(u1, FOLLOWED, 5, NOW.minus(Duration.ofDays(10)));
        follow(u1, followed, lastMonth);
        follow(u2, followed, lastMonth);
        follow(u3, followed, lastMonth);
        follow(u1, newFollow, thisWeek);
        reaction(u1, FINISHED, 12, thisWeek);
        reaction(u2, FINISHED, 12, thisWeek);
        reaction(u3, FINISHED, 12, thisWeek);
        follow(u1, finished, thisWeek);
    }

    @Test
    void ranksAiringShowsByRecentReactionsAndFollowers() {
        List<HypeEntry> ours = ourEntries(service.rank(NOW, 1_000));

        assertEquals(List.of(REACTED, NEW_FOLLOW, FOLLOWED), ours.stream().map(e -> e.media().getId()).toList());

        HypeEntry top = ours.get(0);
        assertEquals(6, top.score());
        assertEquals(2, top.recentReactions());
        assertEquals(0, top.followers());

        HypeEntry followed = ours.get(2);
        assertEquals(3, followed.score());
        assertEquals(0, followed.recentReactions()); // the 10-day-old reaction is outside the window
        assertEquals(0, followed.newFollowers());
        assertEquals(3, followed.followers());

        assertEquals(1, ours.get(1).newFollowers());
        assertEquals(3, ours.get(1).score());
    }

    @Test
    void reactionsAgeOutOfTheWindow() {
        List<HypeEntry> ours = ourEntries(service.rank(NOW.plus(Duration.ofDays(6)), 1_000));

        // A week later REACTED's reactions and NEW_FOLLOW's follow are no longer recent
        assertEquals(List.of(FOLLOWED, NEW_FOLLOW), ours.stream().map(e -> e.media().getId()).toList());
    }

    private static List<HypeEntry> ourEntries(List<HypeEntry> all) {
        return all.stream().filter(e -> TEST_SHOWS.contains(e.media().getId())).toList();
    }

    private void reaction(UserEntity user, long mediaId, int episode, Instant at) {
        EpisodeReactionEntity r = new EpisodeReactionEntity();
        r.setUser(user);
        r.setMediaId(mediaId);
        r.setEpisode(episode);
        r.setReaction(ReactionType.FIRE);
        r.setCreatedAt(at);
        r.setUpdatedAt(at);
        reactionRepository.save(r);
    }

    private void follow(UserEntity user, MediaEntity media, Instant at) {
        FavoriteMediaEntity f = new FavoriteMediaEntity();
        f.setUser(user);
        f.setMedia(media);
        f.setAddedDate(at);
        favoriteMediaRepository.save(f);
    }
}
