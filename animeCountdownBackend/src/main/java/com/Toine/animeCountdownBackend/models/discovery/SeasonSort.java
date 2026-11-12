package com.Toine.animeCountdownBackend.models.discovery;

import java.util.Locale;

public enum SeasonSort {
    SCORE, POPULARITY, NEXT_AIRING;

    /** Parses "score", "popularity" or "next_airing"/"next-airing"; defaults to POPULARITY. */
    public static SeasonSort parse(String value) {
        if (value == null || value.isBlank()) {
            return POPULARITY;
        }
        return SeasonSort.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
