package com.Toine.animeCountdownBackend.services.releases;

import com.Toine.animeCountdownBackend.models.postgreEntities.ReleaseTimeEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseTimesTest {
    private static final Instant NOW = Instant.parse("2026-11-12T12:00:00Z");

    private static ReleaseTimeEntity row(String type, int episode, String at) {
        ReleaseTimeEntity r = new ReleaseTimeEntity();
        r.setMediaId(1L);
        r.setAirType(type);
        r.setEpisode(episode);
        r.setReleaseAt(Instant.parse(at));
        r.setStreamsJson("{\"crunchyroll\":\"https://crunchyroll.com/series/x\",\"amazon\":\"https://amazon.com/y\"}");
        return r;
    }

    @Test
    void parsesAniListIdsFromWebsites() {
        assertEquals(Optional.of(154587L), ReleaseTimes.aniListId(Map.of("aniList", "anilist.co/anime/154587")));
        assertEquals(Optional.of(21L), ReleaseTimes.aniListId(Map.of("aniList", "https://anilist.co/anime/21/ONE-PIECE")));
        assertTrue(ReleaseTimes.aniListId(Map.of("mal", "myanimelist.net/anime/1")).isEmpty());
        assertTrue(ReleaseTimes.aniListId(null).isEmpty());
    }

    @Test
    void parsesDatesAndTreatsYearOneAsUnknown() {
        assertEquals(Optional.of(Instant.parse("2026-11-14T15:30:00Z")), ReleaseTimes.instant("2026-11-14T15:30:00Z"));
        assertEquals(Optional.of(Instant.parse("2026-11-14T15:30:00Z")), ReleaseTimes.instant("2026-11-14T10:30:00-05:00"));
        assertTrue(ReleaseTimes.instant("0001-01-01T00:00:00Z").isEmpty());
        assertTrue(ReleaseTimes.instant("not a date").isEmpty());
    }

    @Test
    void addsHttpsToStreamUrlsAndDropsBlanks() {
        assertEquals(Map.of("crunchyroll", "https://crunchyroll.com/series/x", "netflix", "https://netflix.com/title/1"),
                ReleaseTimes.normalizeStreams(Map.of("crunchyroll", "crunchyroll.com/series/x",
                        "netflix", "https://netflix.com/title/1", "hulu", " ")));
    }

    @Test
    void picksNextReleasePerTypeOrTheLatestOne() {
        var picked = ReleaseTimes.pick(List.of(
                row("SUB", 5, "2026-11-05T15:30:00Z"),   // last week
                row("SUB", 6, "2026-11-12T11:00:00Z"),   // came out an hour ago: still "the" release
                row("SUB", 7, "2026-11-19T15:30:00Z"),
                row("DUB", 2, "2026-11-01T18:00:00Z"),   // no upcoming dub: show the latest
                row("DUB", 1, "2026-10-25T18:00:00Z")), NOW);

        assertEquals(6, picked.get("SUB").episode());
        assertEquals(2, picked.get("DUB").episode());
        assertEquals(List.of("amazon", "crunchyroll"), picked.get("SUB").streams().stream().map(ReleaseTimes.Stream::platform).toList());
        assertEquals("Prime Video", picked.get("SUB").streams().get(0).label());
    }

    @Test
    void reportsDelaysOnlyWhileTheyLast() {
        ReleaseTimeEntity delayed = row("SUB", 8, "2026-11-26T15:30:00Z");
        delayed.setDelayedUntil(Instant.parse("2026-11-26T00:00:00Z"));
        delayed.setDelayNote("Delayed a week (sports broadcast)");
        ReleaseTimeEntity old = row("DUB", 3, "2026-11-20T18:00:00Z");
        old.setDelayedUntil(Instant.parse("2026-11-01T00:00:00Z"));
        old.setDelayNote("Old delay");

        var picked = ReleaseTimes.pick(List.of(delayed, old), NOW);

        assertEquals("Delayed a week (sports broadcast)", picked.get("SUB").delayNote());
        assertNull(picked.get("DUB").delayNote());
    }
}
