package com.Toine.animeCountdownBackend.services.releases;

import com.Toine.animeCountdownBackend.services.releases.AnimeScheduleModels.TimetableEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnimeScheduleModelsTest {

    private final ObjectMapper json = new ObjectMapper();

    /** Shape of a real /timetables/sub entry (Nov 2026): streams is a list, not an object. */
    @Test
    void parsesTimetableEntryWithStreamList() throws Exception {
        TimetableEntry e = json.readValue("""
                {"title":"Show","route":"show","romaji":"Show","english":"Show EN",
                 "delayedFrom":"0001-01-01T00:00:00Z","delayedUntil":"0001-01-01T00:00:00Z",
                 "status":"Ongoing","episodeDate":"2026-11-16T12:31:00Z","episodeNumber":7,"episodes":12,
                 "airType":"sub","mediaTypes":[{"name":"TV","route":"tv"}],
                 "streams":[{"platform":"apple","url":"apple.co/4hGRlzf","name":"Apple TV"},
                            {"platform":"crunchyroll","url":"www.crunchyroll.com/series/x","name":"Crunchyroll"},
                            {"platform":"crunchyroll","url":"www.crunchyroll.com/other","name":"Crunchyroll"}],
                 "airingStatus":"aired"}
                """, TimetableEntry.class);

        assertEquals("show", e.route());
        assertEquals(7, e.episodeNumber());
        assertEquals(Map.of("apple", "apple.co/4hGRlzf", "crunchyroll", "www.crunchyroll.com/series/x"), e.streamUrls());
    }

    @Test
    void stillReadsStreamsSentAsAnObject() throws Exception {
        TimetableEntry e = json.readValue("""
                {"route":"show","streams":{"netflix":"netflix.com/title/1","hidive":""}}
                """, TimetableEntry.class);

        assertEquals("netflix.com/title/1", e.streamUrls().get("netflix"));
    }

    @Test
    void missingStreamsIsEmpty() throws Exception {
        assertTrue(json.readValue("{\"route\":\"show\"}", TimetableEntry.class).streamUrls().isEmpty());
    }
}
