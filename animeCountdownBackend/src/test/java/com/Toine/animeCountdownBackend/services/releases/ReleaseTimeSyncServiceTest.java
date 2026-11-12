package com.Toine.animeCountdownBackend.services.releases;

import com.Toine.animeCountdownBackend.models.postgreEntities.AnimeScheduleLinkEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.ReleaseTimeEntity;
import com.Toine.animeCountdownBackend.repositories.AnimeScheduleLinkRepository;
import com.Toine.animeCountdownBackend.repositories.ReleaseTimeRepository;
import com.Toine.animeCountdownBackend.services.releases.AnimeScheduleModels.AnimeDetails;
import com.Toine.animeCountdownBackend.services.releases.AnimeScheduleModels.TimetableEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReleaseTimeSyncServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-12T12:00:00Z");

    private AnimeScheduleClient client;
    private ReleaseTimeRepository releases;
    private AnimeScheduleLinkRepository links;
    private ReleaseTimeSyncService service;

    @BeforeEach
    void setUp() {
        client = mock(AnimeScheduleClient.class);
        releases = mock(ReleaseTimeRepository.class);
        links = mock(AnimeScheduleLinkRepository.class);
        when(links.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(releases.findByMediaIdAndAirTypeAndEpisode(anyLong(), anyString(), anyInt())).thenReturn(Optional.empty());
        service = new ReleaseTimeSyncService(client, releases, links, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static TimetableEntry entry(String route, String airType, int episode, String date) {
        return new TimetableEntry(route, route, null, null, date, episode, airType,
                Map.of("crunchyroll", "crunchyroll.com/series/" + route), null, null, null);
    }

    @Test
    void mapsShowsToAniListOnceAndSavesReleases() {
        when(links.findAllById(any())).thenReturn(List.of(new AnimeScheduleLinkEntity("frieren", 154587L, NOW)));
        when(client.anime("dandadan")).thenReturn(new AnimeDetails("dandadan", Map.of("aniList", "anilist.co/anime/171018")));

        int saved = service.apply(List.of(
                entry("frieren", "sub", 5, "2026-11-14T15:30:00Z"),
                entry("dandadan", "dub", 3, "2026-11-15T18:00:00Z")), NOW);

        assertEquals(2, saved);
        verify(client, never()).anime("frieren"); // cached link, no lookup
        verify(client).anime("dandadan");
        ArgumentCaptor<ReleaseTimeEntity> captor = ArgumentCaptor.forClass(ReleaseTimeEntity.class);
        verify(releases, times(2)).save(captor.capture());
        ReleaseTimeEntity dub = captor.getAllValues().get(1);
        assertEquals(171018L, dub.getMediaId());
        assertEquals("DUB", dub.getAirType());
        assertEquals(Instant.parse("2026-11-15T18:00:00Z"), dub.getReleaseAt());
        assertTrue(dub.getStreamsJson().contains("https://crunchyroll.com/series/dandadan"));
    }

    @Test
    void skipsShowsWithoutAniListAndUnknownDates() {
        when(links.findAllById(any())).thenReturn(List.of(new AnimeScheduleLinkEntity("donghua-x", null, NOW)));
        when(client.anime("frieren")).thenReturn(new AnimeDetails("frieren", Map.of("aniList", "anilist.co/anime/154587")));

        int saved = service.apply(List.of(
                entry("donghua-x", "sub", 1, "2026-11-14T15:30:00Z"),   // no AniList id
                entry("frieren", "sub", 6, "0001-01-01T00:00:00Z"),      // date unknown
                entry("frieren", "raw?", 6, "2026-11-14T15:30:00Z")), NOW); // unknown air type

        assertEquals(0, saved);
        verify(releases, never()).save(any());
    }

    @Test
    void updatesAnExistingRowInsteadOfDuplicating() {
        when(links.findAllById(any())).thenReturn(List.of(new AnimeScheduleLinkEntity("frieren", 154587L, NOW)));
        ReleaseTimeEntity existing = new ReleaseTimeEntity();
        when(releases.findByMediaIdAndAirTypeAndEpisode(154587L, "SUB", 5)).thenReturn(Optional.of(existing));

        service.apply(List.of(entry("frieren", "sub", 5, "2026-11-21T15:30:00Z")), NOW);

        verify(releases).save(same(existing));
        assertEquals(Instant.parse("2026-11-21T15:30:00Z"), existing.getReleaseAt());
    }
}
