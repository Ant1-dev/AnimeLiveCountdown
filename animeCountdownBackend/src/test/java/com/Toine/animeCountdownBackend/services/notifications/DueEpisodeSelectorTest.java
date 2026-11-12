package com.Toine.animeCountdownBackend.services.notifications;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DueEpisodeSelectorTest {

    private static final Instant AIRS = Instant.parse("2026-11-20T15:00:00Z");
    private final DueEpisodeSelector selector = new DueEpisodeSelector(Duration.ofSeconds(30), Duration.ofMinutes(15));

    @Test
    void atAiringReminderIsDueWithinOneRunBeforeAiring() {
        assertFalse(selector.isDue(AIRS, 0, AIRS.minusSeconds(31)));
        assertTrue(selector.isDue(AIRS, 0, AIRS.minusSeconds(30)));
        assertTrue(selector.isDue(AIRS, 0, AIRS.minusSeconds(1)));
    }

    @Test
    void leadTimeMovesTheSendTimeEarlier() {
        assertFalse(selector.isDue(AIRS, 15, AIRS.minus(Duration.ofMinutes(16))));
        assertTrue(selector.isDue(AIRS, 15, AIRS.minus(Duration.ofMinutes(15))));
        assertTrue(selector.isDue(AIRS, 15, AIRS.minus(Duration.ofMinutes(10))));
    }

    @Test
    void oneDayLeadIsSupported() {
        assertFalse(selector.isDue(AIRS, 1440, AIRS.minus(Duration.ofHours(25))));
        assertTrue(selector.isDue(AIRS, 1440, AIRS.minus(Duration.ofHours(24))));
    }

    @Test
    void missedRemindersStillSendUntilGraceAfterAiring() {
        // Server was down at send time, or the reminder was created inside the lead window
        assertTrue(selector.isDue(AIRS, 60, AIRS.minus(Duration.ofMinutes(5))));
        assertTrue(selector.isDue(AIRS, 0, AIRS.plus(Duration.ofMinutes(14))));
        assertFalse(selector.isDue(AIRS, 0, AIRS.plus(Duration.ofMinutes(15))));
        assertFalse(selector.isDue(AIRS, 60, AIRS.plus(Duration.ofHours(2))));
    }

    @Test
    void candidatesWithoutEpisodeOrTimeAreNeverDue() {
        assertFalse(selector.isDue(null, 0, AIRS));
        ReminderCandidate noEpisode = new ReminderCandidate(1L, 2L, 0, true, false, AIRS, null, "A", "A", null);
        assertFalse(selector.isDue(noEpisode, AIRS));
    }

    @Test
    void queryWindowCoversEveryPossiblyDueEpisode() {
        Instant now = AIRS.minus(Duration.ofHours(24));
        assertEquals(now.minus(Duration.ofMinutes(15)), selector.windowStart(now));
        Instant end = selector.windowEnd(now, 1440);
        assertTrue(selector.isDue(end, 1440, now));
        assertFalse(selector.isDue(end.plusSeconds(1), 1440, now));
    }

    @Test
    void describesTimeLeft() {
        assertEquals("Episode 5 airs in 15 minutes", DueEpisodeSelector.describe(5, AIRS, AIRS.minus(Duration.ofMinutes(15))));
        assertEquals("Episode 5 airs in 1 minute", DueEpisodeSelector.describe(5, AIRS, AIRS.minusSeconds(61)));
        assertEquals("Episode 5 airs in 2 hours", DueEpisodeSelector.describe(5, AIRS, AIRS.minus(Duration.ofMinutes(120))));
        assertEquals("Episode 5 is out now", DueEpisodeSelector.describe(5, AIRS, AIRS.minusSeconds(30)));
        assertEquals("Episode 5 is out now", DueEpisodeSelector.describe(5, AIRS, AIRS.plusSeconds(300)));
    }
}
