package com.Toine.animeCountdownBackend.services.releases;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
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
     * @param streams     where to watch: a list of {"platform", "url", "name"} (URLs without a scheme).
     *                    Kept as JSON so an older {platform: url} object shape still parses; read it
     *                    through {@link #streamUrls()}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TimetableEntry(String route, String title, String english, String romaji, String episodeDate,
                                 Integer episodeNumber, String airType, JsonNode streams,
                                 String delayedFrom, String delayedUntil, String delayedText) {

        /** Platform -> URL (as sent, without a scheme), first URL per platform. */
        public Map<String, String> streamUrls() {
            Map<String, String> urls = new LinkedHashMap<>();
            if (streams == null) {
                return urls;
            }
            if (streams.isArray()) {
                for (JsonNode s : streams) {
                    String platform = s.path("platform").asText("");
                    String url = s.path("url").asText("");
                    if (!platform.isBlank() && !url.isBlank()) {
                        urls.putIfAbsent(platform, url);
                    }
                }
            } else if (streams.isObject()) {
                streams.fields().forEachRemaining(f -> {
                    if (f.getValue().isTextual()) {
                        urls.putIfAbsent(f.getKey(), f.getValue().asText());
                    }
                });
            }
            return urls;
        }
    }

    /**
     * A show (GET /anime/{route}); websites.aniList is how we map to AniList ids. Most websites values
     * are URL strings, but some aren't (websites.streams is a list), so values are left untyped.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AnimeDetails(String route, Map<String, ?> websites) {
    }
}
