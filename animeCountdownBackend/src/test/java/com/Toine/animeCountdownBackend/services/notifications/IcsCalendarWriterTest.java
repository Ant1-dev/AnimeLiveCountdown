package com.Toine.animeCountdownBackend.services.notifications;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IcsCalendarWriterTest {

    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00.123Z");
    private final IcsCalendarWriter writer = new IcsCalendarWriter("https://animelivecountdown.com/", "animelivecountdown.com");

    @Test
    void writesOneUtcEventPerEpisodeSortedByTime() {
        String ics = writer.write(List.of(
                new IcsCalendarWriter.Episode(2L, 7, "Later Show", Instant.parse("2026-11-14T09:30:00Z"), 0),
                new IcsCalendarWriter.Episode(1L, 5, "Frieren", Instant.parse("2026-11-13T15:00:00Z"), 24)), NOW);

        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\nVERSION:2.0\r\n"));
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"));
        assertTrue(ics.contains("\r\nUID:1-5@animelivecountdown.com\r\n"));
        assertTrue(ics.contains("\r\nDTSTAMP:20261111T120000Z\r\n"));
        assertTrue(ics.contains("\r\nDTSTART:20261113T150000Z\r\nDTEND:20261113T152400Z\r\n"));
        assertTrue(ics.contains("\r\nSUMMARY:Frieren - Episode 5\r\n"));
        assertTrue(ics.contains("\r\nURL:https://animelivecountdown.com/media/1\r\n"));
        // Unknown duration falls back to 24 minutes
        assertTrue(ics.contains("\r\nDTSTART:20261114T093000Z\r\nDTEND:20261114T095400Z\r\n"));
        assertTrue(ics.indexOf("UID:1-5@") < ics.indexOf("UID:2-7@"));
        assertEquals(2, ics.split("BEGIN:VEVENT", -1).length - 1);
        assertTrue(ics.contains("REFRESH-INTERVAL;VALUE=DURATION:PT1H"));
    }

    @Test
    void emptyFeedIsStillAValidCalendar() {
        String ics = writer.write(List.of(), NOW);
        assertTrue(ics.contains("BEGIN:VCALENDAR") && ics.contains("END:VCALENDAR"));
        assertFalse(ics.contains("VEVENT"));
    }

    @Test
    void escapesTextValues() {
        assertEquals("a\\, b\\; c\\\\d\\ne", IcsCalendarWriter.escape("a, b; c\\d\ne"));
        String ics = writer.write(List.of(new IcsCalendarWriter.Episode(3L, 1, "Re:Zero, Season 3; Part 2",
                Instant.parse("2026-11-13T15:00:00Z"), 25)), NOW);
        assertTrue(ics.contains("SUMMARY:Re:Zero\\, Season 3\\; Part 2 - Episode 1"));
    }

    @Test
    void foldsLongLinesAt75OctetsWithoutSplittingCharacters() {
        String title = "とても長いアニメのタイトル".repeat(4) + " with a long English subtitle too";
        String ics = writer.write(List.of(new IcsCalendarWriter.Episode(4L, 12, title,
                Instant.parse("2026-11-13T15:00:00Z"), 24)), NOW);

        for (String physical : ics.split("\r\n")) {
            assertTrue(physical.getBytes(StandardCharsets.UTF_8).length <= 75, "line too long: " + physical);
        }
        // Unfolding (remove CRLF + space) restores the original value
        String unfolded = ics.replace("\r\n ", "");
        assertTrue(unfolded.contains("SUMMARY:" + title + " - Episode 12\r\n"));
        assertFalse(ics.contains("�"));
    }
}
