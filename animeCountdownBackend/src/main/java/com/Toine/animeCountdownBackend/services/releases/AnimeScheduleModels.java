package com.Toine.animeCountdownBackend.services.releases;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/** The parts of AnimeSchedule.net's API (v3) we read. Unknown fields are ignored. */
public final class AnimeScheduleModels {
    private AnimeScheduleModels() {
    }

    /**
     * One episode in a timetable (GET /timetables/{raw|sub|dub}).
     *
     * @param route       AnimeSchedule's id for the show (its URL slug)
     * @param episodeDate ISO-8601 UTC; "0001-01-01T00:00:00Z" means unknown
     * @param airType     "raw", "sub" or "dub"
     * @param streams     platform (crunchyroll, netflix, ...) -> URL, without a scheme
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TimetableEntry(String route, String title, String english, String romaji, String episodeDate,
                                 Integer episodeNumber, String airType, Map<String, String> streams,
                                 String delayedFrom, String delayedUntil, String delayedText) {
    }

    /** A show (GET /anime/{route}); websites.aniList is how we map to AniList ids. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AnimeDetails(String route, Map<String, String> websites) {
    }
}
