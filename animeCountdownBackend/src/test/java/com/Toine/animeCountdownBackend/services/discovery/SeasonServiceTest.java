package com.Toine.animeCountdownBackend.services.discovery;

import com.Toine.animeCountdownBackend.models.discovery.SeasonKey;
import com.Toine.animeCountdownBackend.models.discovery.SeasonMedia;
import com.Toine.animeCountdownBackend.models.discovery.SeasonResponse;
import com.Toine.animeCountdownBackend.models.discovery.SeasonSort;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.SeasonCacheEntity;
import com.Toine.animeCountdownBackend.repositories.SeasonCacheRepository;
import com.Toine.animeCountdownBackend.repositories.SeasonMediaInfoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SeasonServiceTest {
    static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z"); // FALL 2026
    static final SeasonKey PAST = new SeasonKey("SPRING", 2024);

    private SeasonMediaInfoRepository mediaInfoRepository;
    private SeasonCacheRepository cacheRepository;
    private AniListSeasonClient aniList;
    private SeasonFetchBudget budget;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private SeasonService service;

    @BeforeEach
    void setUp() {
        mediaInfoRepository = mock(SeasonMediaInfoRepository.class);
        cacheRepository = mock(SeasonCacheRepository.class);
        aniList = mock(AniListSeasonClient.class);
        budget = mock(SeasonFetchBudget.class);
        when(budget.tryAcquire()).thenReturn(true);
        when(cacheRepository.findById(any())).thenReturn(Optional.empty());
        service = new SeasonService(mediaInfoRepository, cacheRepository, aniList, budget, mapper,
                Duration.ofDays(7), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static MediaInfoEntity entity(long id, String genre, int score, long popularity) {
        MediaInfoEntity e = new MediaInfoEntity();
        e.setId(id);
        e.setEngtitle("Local " + id);
        e.setGenres(List.of(genre));
        e.setAvgscore(score);
        e.setPopularity(popularity);
        e.setSeason("FALL");
        e.setSeasonYear(2026);
        return e;
    }

    private static SeasonMedia remote(long id, String genre, int score) {
        return new SeasonMedia(id, "Remote " + id, null, null, List.of(genre), score, 100L * id, 12,
                "FINISHED", "TV", null, null);
    }

    @Test
    void currentSeasonIsServedFromMediaInfoWithoutAniList() {
        when(mediaInfoRepository.findBySeason("FALL", 2026)).thenReturn(List.of(
                entity(1, "Action", 60, 10), entity(2, "Comedy", 90, 5), entity(3, "Action", 80, 1)));

        SeasonResponse response = service.getSeason(new SeasonKey("FALL", 2026), "action", SeasonSort.SCORE);

        assertEquals("local", response.source());
        assertEquals(List.of(3L, 1L), response.media().stream().map(SeasonMedia::id).toList());
        assertEquals(List.of("Action", "Comedy"), response.genres());
        verifyNoInteractions(aniList, cacheRepository);
    }

    @Test
    void nextSeasonIsAlsoLocal() {
        when(mediaInfoRepository.findBySeason("WINTER", 2027)).thenReturn(List.of());

        SeasonResponse response = service.getSeason(new SeasonKey("WINTER", 2027), null, SeasonSort.POPULARITY);

        assertEquals("local", response.source());
        assertEquals(java.time.LocalDate.of(2027, 1, 1), response.startsAt());
        verifyNoInteractions(aniList);
    }

    @Test
    void pastSeasonUsesFreshCacheWithoutAniList() throws Exception {
        String payload = mapper.writeValueAsString(List.of(remote(1, "Drama", 70), remote(2, "Action", 90)));
        when(cacheRepository.findById("SPRING-2024"))
                .thenReturn(Optional.of(new SeasonCacheEntity("SPRING-2024", payload, NOW.minus(Duration.ofDays(1)))));

        SeasonResponse response = service.getSeason(PAST, "Drama", SeasonSort.POPULARITY);

        assertEquals("anilist", response.source());
        assertEquals(List.of(1L), response.media().stream().map(SeasonMedia::id).toList());
        verifyNoInteractions(aniList);
        verify(budget, never()).tryAcquire();
    }

    @Test
    void uncachedPastSeasonIsFetchedOnceAndCached() throws Exception {
        when(aniList.fetchSeason(PAST)).thenReturn(Mono.just(List.of(remote(1, "Drama", 70), remote(2, "Action", 90))));

        SeasonResponse response = service.getSeason(PAST, null, SeasonSort.SCORE);

        assertEquals(List.of(2L, 1L), response.media().stream().map(SeasonMedia::id).toList());
        ArgumentCaptor<SeasonCacheEntity> saved = ArgumentCaptor.forClass(SeasonCacheEntity.class);
        verify(cacheRepository).save(saved.capture());
        assertEquals("SPRING-2024", saved.getValue().getSeasonKey());
        assertEquals(NOW, saved.getValue().getFetchedAt());
        assertEquals(2, mapper.readValue(saved.getValue().getPayload(), List.class).size());
        verify(budget).tryAcquire();
    }

    @Test
    void staleCacheIsRefreshed() throws Exception {
        String payload = mapper.writeValueAsString(List.of(remote(1, "Drama", 70)));
        when(cacheRepository.findById("SPRING-2024"))
                .thenReturn(Optional.of(new SeasonCacheEntity("SPRING-2024", payload, NOW.minus(Duration.ofDays(8)))));
        when(aniList.fetchSeason(PAST)).thenReturn(Mono.just(List.of(remote(1, "Drama", 70), remote(5, "Drama", 50))));

        SeasonResponse response = service.getSeason(PAST, null, SeasonSort.POPULARITY);

        assertEquals(2, response.media().size());
        verify(aniList).fetchSeason(PAST);
    }

    @Test
    void exhaustedBudgetServesStaleCache() throws Exception {
        String payload = mapper.writeValueAsString(List.of(remote(1, "Drama", 70)));
        when(cacheRepository.findById("SPRING-2024"))
                .thenReturn(Optional.of(new SeasonCacheEntity("SPRING-2024", payload, NOW.minus(Duration.ofDays(30)))));
        when(budget.tryAcquire()).thenReturn(false);

        SeasonResponse response = service.getSeason(PAST, null, SeasonSort.POPULARITY);

        assertEquals(1, response.media().size());
        verifyNoInteractions(aniList);
    }

    @Test
    void exhaustedBudgetWithoutCacheIsUnavailable() {
        when(budget.tryAcquire()).thenReturn(false);

        assertThrows(SeasonUnavailableException.class, () -> service.getSeason(PAST, null, SeasonSort.POPULARITY));
        verifyNoInteractions(aniList);
    }

    @Test
    void aniListFailureWithoutCacheIsUnavailable() {
        when(aniList.fetchSeason(PAST)).thenReturn(Mono.error(new RuntimeException("429 Too Many Requests")));

        assertThrows(SeasonUnavailableException.class, () -> service.getSeason(PAST, null, SeasonSort.POPULARITY));
        verify(cacheRepository, never()).save(any());
    }

    @Test
    void overviewPointsAtNextSeasonStart() {
        var overview = service.overview();
        assertEquals(new SeasonKey("FALL", 2026), overview.current());
        assertEquals(new SeasonKey("WINTER", 2027), overview.next());
        assertEquals(java.time.LocalDate.of(2027, 1, 1), overview.nextStartsAt());
    }
}
