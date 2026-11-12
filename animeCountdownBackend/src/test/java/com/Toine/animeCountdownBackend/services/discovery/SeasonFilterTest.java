package com.Toine.animeCountdownBackend.services.discovery;

import com.Toine.animeCountdownBackend.models.discovery.SeasonMedia;
import com.Toine.animeCountdownBackend.models.discovery.SeasonSort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SeasonFilterTest {
    static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

    static SeasonMedia show(long id, List<String> genres, Integer score, Long popularity, Instant airingAt) {
        return new SeasonMedia(id, "Show " + id, null, null, genres, score, popularity, 12, "RELEASING", "TV",
                airingAt == null ? null : 3, airingAt);
    }

    private final List<SeasonMedia> media = List.of(
            show(1, List.of("Action", "Drama"), 70, 5000L, NOW.plusSeconds(7200)),
            show(2, List.of("Comedy"), 85, 1000L, NOW.plusSeconds(600)),
            show(3, List.of("action"), null, 9000L, null),
            show(4, List.of("Drama"), 85, 3000L, NOW.minusSeconds(60)));

    private static List<Long> ids(List<SeasonMedia> media) {
        return media.stream().map(SeasonMedia::id).toList();
    }

    @Test
    void filtersByGenreIgnoringCase() {
        assertEquals(List.of(3L, 1L), ids(SeasonFilter.apply(media, "ACTION", SeasonSort.POPULARITY, NOW)));
        assertEquals(List.of(), ids(SeasonFilter.apply(media, "Horror", SeasonSort.POPULARITY, NOW)));
    }

    @Test
    void blankGenreKeepsEverything() {
        assertEquals(4, SeasonFilter.apply(media, " ", SeasonSort.POPULARITY, NOW).size());
        assertEquals(4, SeasonFilter.apply(media, null, SeasonSort.POPULARITY, NOW).size());
    }

    @Test
    void sortsByPopularityDescending() {
        assertEquals(List.of(3L, 1L, 4L, 2L), ids(SeasonFilter.apply(media, null, SeasonSort.POPULARITY, NOW)));
    }

    @Test
    void sortsByScoreWithUnscoredLastAndPopularityTieBreak() {
        assertEquals(List.of(4L, 2L, 1L, 3L), ids(SeasonFilter.apply(media, null, SeasonSort.SCORE, NOW)));
    }

    @Test
    void sortsByNextAiringSoonestFirstAndPastOrMissingLast() {
        // 4 aired a minute ago, so it's treated like a show with no upcoming episode
        assertEquals(List.of(2L, 1L, 3L, 4L), ids(SeasonFilter.apply(media, null, SeasonSort.NEXT_AIRING, NOW)));
    }

    @Test
    void listsDistinctGenresAlphabetically() {
        assertEquals(List.of("Action", "Comedy", "Drama"), SeasonFilter.genres(media));
    }

    @Test
    void parsesSortNames() {
        assertEquals(SeasonSort.NEXT_AIRING, SeasonSort.parse("next-airing"));
        assertEquals(SeasonSort.SCORE, SeasonSort.parse("score"));
        assertEquals(SeasonSort.POPULARITY, SeasonSort.parse(null));
        assertThrows(IllegalArgumentException.class, () -> SeasonSort.parse("random"));
    }
}
