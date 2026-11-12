package com.Toine.animeCountdownBackend.services;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LatestEpisodeTest {

    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");
    private static final Instant FUTURE = NOW.plus(Duration.ofDays(2));
    private static final Instant PAST = NOW.minus(Duration.ofMinutes(3));

    @Test
    void releasingShowsLatestIsTheOneBeforeNext() {
        assertEquals(Optional.of(4), LatestEpisode.latestAired("RELEASING", 5, FUTURE, 12, NOW));
    }

    @Test
    void storedNextEpisodeCountsOnceItsTimeHasPassed() {
        // The sync hasn't moved on to episode 6 yet, but episode 5 has aired
        assertEquals(Optional.of(5), LatestEpisode.latestAired("RELEASING", 5, PAST, 12, NOW));
    }

    @Test
    void premiereNotAiredYetMeansNoEpisode() {
        assertEquals(Optional.empty(), LatestEpisode.latestAired("RELEASING", 1, FUTURE, 12, NOW));
        assertEquals(Optional.empty(), LatestEpisode.latestAired("NOT_YET_RELEASED", 1, FUTURE, 12, NOW));
        assertEquals(Optional.empty(), LatestEpisode.latestAired("NOT_YET_RELEASED", null, null, null, NOW));
    }

    @Test
    void finishedShowUsesTotalEpisodes() {
        assertEquals(Optional.of(12), LatestEpisode.latestAired("FINISHED", null, null, 12, NOW));
        assertEquals(Optional.of(12), LatestEpisode.latestAired("FINISHED", 12, PAST, 12, NOW));
    }

    @Test
    void finishedWithoutTotalFallsBackToNextEpisode() {
        assertEquals(Optional.of(11), LatestEpisode.latestAired("FINISHED", 12, FUTURE, null, NOW));
    }

    @Test
    void unknownScheduleMeansNoEpisode() {
        assertEquals(Optional.empty(), LatestEpisode.latestAired("RELEASING", null, null, null, NOW));
    }
}
