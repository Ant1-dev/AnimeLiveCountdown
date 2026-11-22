package com.Toine.animeCountdownBackend.services.watch;

import com.Toine.animeCountdownBackend.models.discovery.StreamingLink;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.ReleaseTimeEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.MediaInfoRepository;
import com.Toine.animeCountdownBackend.repositories.ReleaseTimeRepository;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import com.Toine.animeCountdownBackend.repositories.UserStreamingServicesRepository;
import com.Toine.animeCountdownBackend.services.watch.MyServices.CustomPlatform;
import com.Toine.animeCountdownBackend.services.watch.MyServices.Selection;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Saving picks and the availability lookup against the real (local) Postgres schema. Each test runs in
 * a transaction that is rolled back, so nothing is left in the shared dev database.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=update")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MyServicesServiceTest {

    // Far above any AniList id, so they never collide with synced shows
    private static final long ANILIST_ONLY = 990_000_301L;
    private static final long BOTH_SOURCES = 990_000_302L;
    private static final long NO_DATA = 990_000_303L;
    private static final long UNLISTED_ONLY = 990_000_304L;
    private static final long OLD_RELEASE_ONLY = 990_000_305L;
    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");

    @Autowired private UserStreamingServicesRepository repository;
    @Autowired private MediaInfoRepository mediaInfoRepository;
    @Autowired private ReleaseTimeRepository releaseTimeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager entityManager;

    private MyServicesService service;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        service = new MyServicesService(repository, mediaInfoRepository, releaseTimeRepository, entityManager,
                Clock.fixed(NOW, ZoneOffset.UTC));
        user = new UserEntity();
        user.setEmail("services-" + UUID.randomUUID() + "@example.test");
        user.setName("viewer");
        user = userRepository.save(user);
    }

    @Test
    void nothingSavedIsEmpty() {
        assertEquals(Selection.empty(), service.get(user.getId()));
    }

    @Test
    void savedPicksRoundTripAndCanBeReplaced() {
        service.save(user, new Selection(List.of("netflix", "crunchyroll"),
                List.of(new CustomPlatform("Plex", "https://app.plex.tv/search?q={title}"))));
        entityManager.flush();
        entityManager.clear();

        Selection saved = service.get(user.getId());
        assertEquals(List.of("crunchyroll", "netflix"), saved.services());
        assertEquals(List.of(new CustomPlatform("Plex", "https://app.plex.tv/search?q={title}")), saved.customPlatforms());

        service.save(user, new Selection(List.of("hidive"), List.of()));
        entityManager.flush();
        entityManager.clear();
        assertEquals(new Selection(List.of("hidive"), List.of()), service.get(user.getId()));
    }

    @Test
    void invalidPicksAreNotSaved() {
        assertThrows(MyServices.InvalidSelectionException.class,
                () -> service.save(user, new Selection(List.of("nope"), List.of())));
        assertTrue(repository.findById(user.getId()).isEmpty());
    }

    @Test
    void availabilityCombinesAniListLinksAndReleaseStreams() {
        mediaInfoRepository.save(mediaInfo(ANILIST_ONLY, List.of(link("Amazon Prime Video"), link("Disney Plus"))));
        mediaInfoRepository.save(mediaInfo(BOTH_SOURCES, List.of(link("Crunchyroll"))));
        mediaInfoRepository.save(mediaInfo(NO_DATA, null));
        mediaInfoRepository.save(mediaInfo(UNLISTED_ONLY, List.of(link("Laftel"))));
        releaseTimeRepository.save(release(BOTH_SOURCES, "{\"netflix\":\"https://netflix.com/title/1\"}", NOW.plus(Duration.ofDays(2))));
        releaseTimeRepository.save(release(OLD_RELEASE_ONLY, "{\"hulu\":\"https://hulu.com/x\"}", NOW.minus(Duration.ofDays(60))));
        entityManager.flush();
        entityManager.clear();

        Map<Long, List<String>> availability = new java.util.HashMap<>();
        service.availability(List.of(ANILIST_ONLY, BOTH_SOURCES, NO_DATA, UNLISTED_ONLY, OLD_RELEASE_ONLY))
                .forEach((id, links) -> availability.put(id, links.stream().map(StreamingServices.ServiceLink::key).toList()));

        assertEquals(List.of("disney", "amazon"), availability.get(ANILIST_ONLY));
        assertEquals(List.of("crunchyroll", "netflix"), availability.get(BOTH_SOURCES));
        assertEquals(List.of(), availability.get(UNLISTED_ONLY), "has data, none of it on a catalog service");
        assertFalse(availability.containsKey(NO_DATA), "no data means unknown");
        assertFalse(availability.containsKey(OLD_RELEASE_ONLY), "old releases are ignored");
    }

    @Test
    void availabilityOfNothingIsEmpty() {
        assertEquals(Map.of(), service.availability(List.of()));
    }

    private static StreamingLink link(String site) {
        return new StreamingLink(site, "https://example.com/" + site.replace(' ', '-'), null, null, "English");
    }

    private static MediaInfoEntity mediaInfo(long id, List<StreamingLink> links) {
        MediaInfoEntity m = new MediaInfoEntity();
        m.setId(id);
        m.setRomtitle("Test Show " + id);
        m.setPopularity(0L);
        m.setStreamingLinks(links);
        return m;
    }

    private static ReleaseTimeEntity release(long mediaId, String streams, Instant at) {
        ReleaseTimeEntity r = new ReleaseTimeEntity();
        r.setMediaId(mediaId);
        r.setAirType("SUB");
        r.setEpisode(1);
        r.setReleaseAt(at);
        r.setStreamsJson(streams);
        r.setUpdatedAt(at);
        return r;
    }
}
