package com.Toine.animeCountdownBackend.services.share;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CountdownTextTest {

    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

    @Test
    void daysAndHours() {
        Instant at = NOW.plus(Duration.ofDays(2).plusHours(4).plusMinutes(59));
        assertEquals("Ep 5 airs in 2d 4h", CountdownText.line("RELEASING", 5, at, 12, NOW));
    }

    @Test
    void hoursAndMinutesThenMinutes() {
        assertEquals("Ep 1 airs in 3h 7m",
                CountdownText.line("NOT_YET_RELEASED", 1, NOW.plus(Duration.ofMinutes(187)), null, NOW));
        assertEquals("Ep 9 airs in 42m", CountdownText.line("RELEASING", 9, NOW.plusSeconds(42 * 60 + 30), null, NOW));
        assertEquals("Ep 9 airs in 1m", CountdownText.line("RELEASING", 9, NOW.plusSeconds(20), null, NOW));
    }

    @Test
    void airedButNotSyncedYet() {
        assertEquals("Ep 5 is out now", CountdownText.line("RELEASING", 5, NOW.minusSeconds(30), 12, NOW));
    }

    @Test
    void statusesWithoutACountdown() {
        assertEquals("Finished airing · 12 episodes", CountdownText.line("FINISHED", null, null, 12, NOW));
        assertEquals("Finished airing", CountdownText.line("FINISHED", null, null, null, NOW));
        assertEquals("Coming soon", CountdownText.line("NOT_YET_RELEASED", null, null, null, NOW));
        assertEquals("On hiatus", CountdownText.line("HIATUS", null, null, null, NOW));
        assertEquals("Airing date TBA", CountdownText.line("RELEASING", null, null, null, NOW));
    }

    @Test
    void futureDateWithoutEpisodeNumber() {
        assertEquals("Airs in 1d 0h", CountdownText.line("NOT_YET_RELEASED", null, NOW.plus(Duration.ofDays(1)), null, NOW));
    }
}
