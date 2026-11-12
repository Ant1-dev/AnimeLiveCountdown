package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.ScheduleNote;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleNoteDetectorTest {

    private static final Duration WEEK = Duration.ofDays(7);
    // Episode 5 of a weekly show was scheduled for this time
    private static final Instant EP5 = Instant.parse("2026-11-06T15:00:00Z");

    @Test
    void justAiredNormalAdvanceIsNotFlagged() {
        MediaEntity stored = media("RELEASING", 5, EP5);
        MediaEntity fresh = media("RELEASING", 6, EP5.plus(WEEK));

        ScheduleNoteDetector.detect(stored, fresh, EP5.plusSeconds(60));

        assertNull(stored.getScheduleNoteType());
        assertEquals(EP5, stored.getLastAiredAt());
    }

    @Test
    void weeklyShowSkippingAWeekIsOnBreak() {
        MediaEntity stored = media("RELEASING", 5, EP5);
        Instant returns = EP5.plus(WEEK.multipliedBy(2));
        Instant now = EP5.plusSeconds(60);

        ScheduleNoteDetector.detect(stored, media("RELEASING", 6, returns), now);

        assertEquals(ScheduleNote.Type.ON_BREAK, stored.getScheduleNoteType());
        assertEquals(now, stored.getScheduleNoteDetectedAt());
        assertEquals(EP5, stored.getScheduleNoteOriginalAt());
        assertEquals(returns, stored.getScheduleNoteExpiresAt());
    }

    @Test
    void breakIsMeasuredFromLastAiringWhenAniListHadNoNextDateInBetween() {
        MediaEntity stored = media("RELEASING", 5, EP5);
        // Aired, but AniList has no next episode yet
        ScheduleNoteDetector.detect(stored, media("RELEASING", null, null), EP5.plusSeconds(60));
        assertEquals(EP5, stored.getLastAiredAt());
        stored.setNext_Airing_At(null);
        stored.setNext_Airing_Episode(null);

        Instant returns = EP5.plus(Duration.ofDays(21));
        ScheduleNoteDetector.detect(stored, media("RELEASING", 6, returns), EP5.plus(Duration.ofDays(2)));

        assertEquals(ScheduleNote.Type.ON_BREAK, stored.getScheduleNoteType());
        assertEquals(returns, stored.getScheduleNoteExpiresAt());
    }

    @Test
    void severalEpisodesAiringBetweenSyncsIsNotABreak() {
        // Sync was down for two weeks: episodes 5 and 6 aired unseen, episode 7 is next
        MediaEntity stored = media("RELEASING", 5, EP5);
        Instant ep7 = EP5.plus(WEEK.multipliedBy(2));

        ScheduleNoteDetector.detect(stored, media("RELEASING", 7, ep7), EP5.plus(Duration.ofDays(10)));

        assertNull(stored.getScheduleNoteType());
        assertEquals(EP5.plus(WEEK), stored.getLastAiredAt());
    }

    @Test
    void secondSkippedWeekInARowIsStillABreak() {
        Instant ep6 = EP5.plus(WEEK.multipliedBy(2));
        Instant ep7 = ep6.plus(WEEK.multipliedBy(2));
        MediaEntity stored = media("RELEASING", 5, EP5);
        ScheduleNoteDetector.detect(stored, media("RELEASING", 6, ep6), EP5.plusSeconds(60));
        stored.setNext_Airing_At(ep6);
        stored.setNext_Airing_Episode(6);

        ScheduleNoteDetector.detect(stored, media("RELEASING", 7, ep7), ep6.plusSeconds(60));

        assertEquals(ScheduleNote.Type.ON_BREAK, stored.getScheduleNoteType());
        assertEquals(ep7, stored.getScheduleNoteExpiresAt());
    }

    @Test
    void episodePushedBackIsDelayed() {
        MediaEntity stored = media("RELEASING", 6, EP5.plus(WEEK));
        Instant now = EP5.plus(Duration.ofDays(3));
        Instant newTime = EP5.plus(WEEK.multipliedBy(2));

        ScheduleNoteDetector.detect(stored, media("RELEASING", 6, newTime), now);

        assertEquals(ScheduleNote.Type.DELAYED, stored.getScheduleNoteType());
        assertEquals(EP5.plus(WEEK), stored.getScheduleNoteOriginalAt());
        assertEquals(newTime, stored.getScheduleNoteExpiresAt());
        assertEquals(now, stored.getScheduleNoteDetectedAt());
    }

    @Test
    void secondPushKeepsTheOriginalTime() {
        Instant original = EP5.plus(WEEK);
        MediaEntity stored = media("RELEASING", 6, original);
        Instant firstSeen = EP5.plus(Duration.ofDays(1));
        ScheduleNoteDetector.detect(stored, media("RELEASING", 6, original.plus(WEEK)), firstSeen);
        stored.setNext_Airing_At(original.plus(WEEK));

        ScheduleNoteDetector.detect(stored, media("RELEASING", 6, original.plus(WEEK.multipliedBy(2))), EP5.plus(Duration.ofDays(2)));

        assertEquals(ScheduleNote.Type.DELAYED, stored.getScheduleNoteType());
        assertEquals(original, stored.getScheduleNoteOriginalAt());
        assertEquals(firstSeen, stored.getScheduleNoteDetectedAt());
        assertEquals(original.plus(WEEK.multipliedBy(2)), stored.getScheduleNoteExpiresAt());
    }

    @Test
    void smallTimeCorrectionIsNotADelay() {
        MediaEntity stored = media("RELEASING", 6, EP5.plus(WEEK));

        ScheduleNoteDetector.detect(stored, media("RELEASING", 6, EP5.plus(WEEK).plus(Duration.ofMinutes(30))), EP5);

        assertNull(stored.getScheduleNoteType());
    }

    @Test
    void premiereDateMovingLaterIsDelayed() {
        Instant premiere = Instant.parse("2026-12-01T15:00:00Z");
        Instant moved = Instant.parse("2027-01-08T15:00:00Z");
        MediaEntity stored = media("NOT_YET_RELEASED", 1, premiere);

        ScheduleNoteDetector.detect(stored, media("NOT_YET_RELEASED", 1, moved), EP5);

        assertEquals(ScheduleNote.Type.DELAYED, stored.getScheduleNoteType());
        assertEquals(premiere, stored.getScheduleNoteOriginalAt());
        assertEquals(moved, stored.getScheduleNoteExpiresAt());
    }

    @Test
    void premiereTimeRefinedWithinADayIsNotADelay() {
        Instant placeholder = Instant.parse("2026-12-01T00:00:00Z");
        MediaEntity stored = media("NOT_YET_RELEASED", 1, placeholder);

        ScheduleNoteDetector.detect(stored, media("NOT_YET_RELEASED", 1, placeholder.plus(Duration.ofHours(15))), EP5);

        assertNull(stored.getScheduleNoteType());
    }

    @Test
    void returningFromBreakClearsTheNote() {
        Instant returns = EP5.plus(WEEK.multipliedBy(2));
        MediaEntity stored = media("RELEASING", 5, EP5);
        ScheduleNoteDetector.detect(stored, media("RELEASING", 6, returns), EP5.plusSeconds(60));
        stored.setNext_Airing_At(returns);
        stored.setNext_Airing_Episode(6);

        // Episode 6 airs and episode 7 is a week later: back on schedule
        ScheduleNoteDetector.detect(stored, media("RELEASING", 7, returns.plus(WEEK)), returns.plusSeconds(60));

        assertNull(stored.getScheduleNoteType());
        assertNull(stored.getScheduleNoteExpiresAt());
        assertEquals(returns, stored.getLastAiredAt());
    }

    @Test
    void delayedEpisodeAiringClearsTheNote() {
        Instant original = EP5.plus(WEEK);
        Instant delayedTo = original.plus(WEEK);
        MediaEntity stored = media("RELEASING", 6, original);
        ScheduleNoteDetector.detect(stored, media("RELEASING", 6, delayedTo), EP5.plus(Duration.ofDays(1)));
        stored.setNext_Airing_At(delayedTo);

        ScheduleNoteDetector.detect(stored, media("RELEASING", 7, delayedTo.plus(WEEK)), delayedTo.plusSeconds(60));

        assertNull(stored.getScheduleNoteType());
    }

    @Test
    void delayReversedClearsTheNote() {
        Instant original = EP5.plus(WEEK);
        MediaEntity stored = media("RELEASING", 6, original);
        ScheduleNoteDetector.detect(stored, media("RELEASING", 6, original.plus(WEEK)), EP5.plus(Duration.ofDays(1)));
        stored.setNext_Airing_At(original.plus(WEEK));

        ScheduleNoteDetector.detect(stored, media("RELEASING", 6, original), EP5.plus(Duration.ofDays(2)));

        assertNull(stored.getScheduleNoteType());
    }

    @Test
    void breakReturnDatePushedBackStaysOnBreak() {
        Instant returns = EP5.plus(WEEK.multipliedBy(2));
        MediaEntity stored = media("RELEASING", 5, EP5);
        ScheduleNoteDetector.detect(stored, media("RELEASING", 6, returns), EP5.plusSeconds(60));
        stored.setNext_Airing_At(returns);
        stored.setNext_Airing_Episode(6);

        ScheduleNoteDetector.detect(stored, media("RELEASING", 6, returns.plus(WEEK)), EP5.plus(Duration.ofDays(3)));

        assertEquals(ScheduleNote.Type.ON_BREAK, stored.getScheduleNoteType());
        assertEquals(returns.plus(WEEK), stored.getScheduleNoteExpiresAt());
    }

    @Test
    void expiredNoteIsHiddenFromClients() {
        MediaEntity stored = media("RELEASING", 6, EP5.plus(WEEK));
        ScheduleNoteDetector.detect(stored, media("RELEASING", 6, EP5.plus(WEEK.multipliedBy(2))), EP5);

        assertNotNull(ScheduleNote.activeOf(stored, EP5.plus(WEEK)));
        assertNull(ScheduleNote.activeOf(stored, EP5.plus(WEEK.multipliedBy(2))));
    }

    private static MediaEntity media(String status, Integer episode, Instant nextAiring) {
        MediaEntity m = new MediaEntity();
        m.setId(1L);
        m.setStatus(status);
        m.setNext_Airing_Episode(episode);
        m.setNext_Airing_At(nextAiring);
        return m;
    }
}
