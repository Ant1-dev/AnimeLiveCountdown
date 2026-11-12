package com.Toine.animeCountdownBackend.services.discovery;

import com.Toine.animeCountdownBackend.models.discovery.SeasonMedia;
import com.Toine.animeCountdownBackend.models.discovery.SeasonSort;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/** Genre filtering and sorting for the season browser. Pure functions, no I/O. */
public final class SeasonFilter {
    private SeasonFilter() {
    }

    private static final Comparator<SeasonMedia> BY_POPULARITY = Comparator.comparing(
            SeasonMedia::popularity, Comparator.nullsLast(Comparator.reverseOrder()));

    /**
     * Keeps shows tagged with {@code genre} (case-insensitive; blank keeps all) and sorts them.
     * SCORE: highest first, unscored last. NEXT_AIRING: soonest upcoming episode first; shows with
     * no future episode go last. Ties fall back to popularity.
     */
    public static List<SeasonMedia> apply(List<SeasonMedia> media, String genre, SeasonSort sort, Instant now) {
        return media.stream()
                .filter(m -> matchesGenre(m, genre))
                .sorted(comparator(sort, now))
                .toList();
    }

    /** Every genre that appears in the list, alphabetically. */
    public static List<String> genres(List<SeasonMedia> media) {
        TreeSet<String> genres = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (SeasonMedia m : media) {
            if (m.genres() != null) {
                m.genres().stream().filter(Objects::nonNull).forEach(genres::add);
            }
        }
        return List.copyOf(genres);
    }

    private static boolean matchesGenre(SeasonMedia media, String genre) {
        if (genre == null || genre.isBlank()) {
            return true;
        }
        return media.genres() != null && media.genres().stream().anyMatch(g -> g != null && g.equalsIgnoreCase(genre.trim()));
    }

    private static Comparator<SeasonMedia> comparator(SeasonSort sort, Instant now) {
        return switch (sort) {
            case POPULARITY -> BY_POPULARITY;
            case SCORE -> Comparator.comparing(SeasonMedia::avgscore, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(BY_POPULARITY);
            case NEXT_AIRING -> Comparator.comparing((SeasonMedia m) -> upcoming(m.airingat(), now),
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(BY_POPULARITY);
        };
    }

    private static Instant upcoming(Instant airingAt, Instant now) {
        return airingAt != null && airingAt.isAfter(now) ? airingAt : null;
    }
}
