package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.components.AniListRateLimiter;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.repositories.FavoriteMediaRepository;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import com.Toine.animeCountdownBackend.services.ScheduleService.SyncScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.graphql.client.HttpGraphQlClient;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class ScheduleServiceSyncTest {

    private static final Instant SOON = Instant.parse("2026-11-20T15:00:00Z");

    private MediaRepository mediaRepository;
    private FavoriteMediaRepository favoriteMediaRepository;
    private HttpGraphQlClient graphQlClient;
    private ScheduleService service;

    @BeforeEach
    void setUp() {
        mediaRepository = mock(MediaRepository.class);
        favoriteMediaRepository = mock(FavoriteMediaRepository.class);
        graphQlClient = mock(HttpGraphQlClient.class);
        service = new ScheduleService(graphQlClient, mediaRepository, favoriteMediaRepository, new AniListRateLimiter(0));
        when(favoriteMediaRepository.findFavoritedMediaIds(anyCollection())).thenReturn(Set.of());
    }

    @Test
    void upcomingSweepNeverRemovesAiringShows() {
        MediaEntity upcoming = media(1L, "NOT_YET_RELEASED", SOON);
        when(mediaRepository.findAllById(any())).thenReturn(List.of(upcoming));
        when(mediaRepository.findUpcoming()).thenReturn(List.of(upcoming));

        service.updateDatabase(List.of(media(1L, "NOT_YET_RELEASED", SOON)), SyncScope.UPCOMING);

        verify(mediaRepository, never()).findNotUpcoming();
        verify(mediaRepository, never()).deleteAllById(any());
    }

    @Test
    void upcomingSweepRemovesUpcomingShowsMissingFromAniList() {
        when(mediaRepository.findAllById(any())).thenReturn(List.of());
        when(mediaRepository.findUpcoming()).thenReturn(List.of(media(2L, "NOT_YET_RELEASED", SOON)));

        service.updateDatabase(List.of(media(1L, "NOT_YET_RELEASED", SOON)), SyncScope.UPCOMING);

        assertEquals(Set.of(2L), capturedDeletes());
    }

    @Test
    void airingSweepRemovesStoppedShowsButKeepsFavoritedOnes() {
        MediaEntity stillAiring = media(1L, "RELEASING", SOON);
        MediaEntity endedFavorited = media(2L, "RELEASING", SOON);
        MediaEntity onHiatus = media(3L, "HIATUS", null);
        when(mediaRepository.findAllById(any())).thenReturn(List.of(stillAiring));
        when(mediaRepository.findNotUpcoming()).thenReturn(List.of(stillAiring, endedFavorited, onHiatus));
        when(favoriteMediaRepository.findFavoritedMediaIds(anyCollection())).thenReturn(Set.of(2L));

        service.updateDatabase(List.of(media(1L, "RELEASING", SOON)), SyncScope.AIRING);

        assertEquals(Set.of(3L), capturedDeletes());
        assertEquals("FINISHED", endedFavorited.getStatus());
        assertNull(endedFavorited.getNext_Airing_At());
        verify(mediaRepository, never()).findUpcoming();
    }

    @Test
    void airingSweepPicksUpShowThatStartedAiring() {
        MediaEntity stored = media(5L, "NOT_YET_RELEASED", SOON);
        when(mediaRepository.findAllById(any())).thenReturn(List.of(stored));
        when(mediaRepository.findNotUpcoming()).thenReturn(List.of());

        service.updateDatabase(List.of(media(5L, "RELEASING", SOON.plusSeconds(3600))), SyncScope.AIRING);

        assertEquals("RELEASING", stored.getStatus());
        assertEquals(SOON.plusSeconds(3600), stored.getNext_Airing_At());
        verify(mediaRepository, never()).deleteAllById(any());
    }

    @Test
    void justAiredUpdatesMoveToNextEpisodeWithoutRemovingAnything() {
        MediaEntity stored = media(7L, "RELEASING", SOON);
        stored.setNext_Airing_Episode(4);
        when(mediaRepository.findAllById(any())).thenReturn(List.of(stored));

        MediaEntity fresh = media(7L, "RELEASING", SOON.plusSeconds(7 * 86400));
        fresh.setNext_Airing_Episode(5);
        int changed = service.applyUpdates(List.of(fresh));

        assertEquals(1, changed);
        assertEquals(5, stored.getNext_Airing_Episode());
        assertEquals(SOON.plusSeconds(7 * 86400), stored.getNext_Airing_At());
        verify(mediaRepository, never()).deleteAllById(any());
        verifyNoInteractions(favoriteMediaRepository);
    }

    @Test
    void justAiredJobMakesNoApiCallsWhenNothingAired() {
        when(mediaRepository.findIdsAiringBetween(any(), any())).thenReturn(List.of());

        service.syncJustAired();

        verifyNoInteractions(graphQlClient);
    }

    @SuppressWarnings("unchecked")
    private Set<Long> capturedDeletes() {
        ArgumentCaptor<Iterable<Long>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(mediaRepository).deleteAllById(captor.capture());
        Set<Long> ids = new HashSet<>();
        captor.getValue().forEach(ids::add);
        return ids;
    }

    private static MediaEntity media(Long id, String status, Instant nextAiring) {
        MediaEntity m = new MediaEntity();
        m.setId(id);
        m.setStatus(status);
        m.setNext_Airing_At(nextAiring);
        m.setTitle_Romaji("Show " + id);
        return m;
    }
}
