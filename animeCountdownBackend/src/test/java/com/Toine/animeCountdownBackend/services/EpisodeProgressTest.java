package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EpisodeProgressTest {
    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

    @Test
    void airingShowHasAiredEverythingBeforeTheNextEpisode() {
        assertEquals(6, EpisodeProgress.latestAired(media("RELEASING", 7, NOW.plusSeconds(3600)), null, NOW));
    }

    @Test
    void nextEpisodeCountsOnceItsTimeHasPassed() {
        // Until the just-aired sync moves the show on, the stored "next" episode has already aired
        assertEquals(7, EpisodeProgress.latestAired(media("RELEASING", 7, NOW.minusSeconds(60)), null, NOW));
    }

    @Test
    void upcomingShowHasNothingAired() {
        assertEquals(0, EpisodeProgress.latestAired(media("NOT_YET_RELEASED", 1, NOW.plusSeconds(86400)), null, NOW));
        assertEquals(0, EpisodeProgress.latestAired(media("NOT_YET_RELEASED", null, null), null, NOW));
    }

    @Test
    void finishedShowUsesEpisodeCountWhenKnown() {
        assertEquals(12, EpisodeProgress.latestAired(media("FINISHED", null, null), 12, NOW));
        assertNull(EpisodeProgress.latestAired(media("FINISHED", null, null), null, NOW));
        assertNull(EpisodeProgress.latestAired(media("HIATUS", null, null), null, NOW));
    }

    @Test
    void behindNeverGoesNegative() {
        assertEquals(3, EpisodeProgress.behind(6, 3));
        assertEquals(0, EpisodeProgress.behind(6, 9));
        assertNull(EpisodeProgress.behind(null, 2));
    }

    static MediaEntity media(String status, Integer nextEpisode, Instant nextAt) {
        MediaEntity m = new MediaEntity();
        m.setId(1L);
        m.setStatus(status);
        m.setNext_Airing_Episode(nextEpisode);
        m.setNext_Airing_At(nextAt);
        return m;
    }
}
