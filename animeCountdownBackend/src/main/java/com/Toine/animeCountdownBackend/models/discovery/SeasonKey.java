package com.Toine.animeCountdownBackend.models.discovery;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * An anime season such as FALL 2026. Seasons follow the usual premiere months: WINTER starts in
 * January, SPRING in April, SUMMER in July and FALL in October (AniList's season names).
 */
public record SeasonKey(String season, int year) implements Comparable<SeasonKey> {
    public static final List<String> SEASONS = List.of("WINTER", "SPRING", "SUMMER", "FALL");
    public static final int MIN_YEAR = 1970;

    public SeasonKey {
        if (season == null || !SEASONS.contains(season)) {
            throw new IllegalArgumentException("Season must be one of " + SEASONS);
        }
    }

    /** Parses user input like "fall", 2026. Throws IllegalArgumentException when invalid. */
    public static SeasonKey parse(String season, int year) {
        if (season == null) {
            throw new IllegalArgumentException("Season is required");
        }
        if (year < MIN_YEAR || year > 9999) {
            throw new IllegalArgumentException("Year must be between " + MIN_YEAR + " and 9999");
        }
        return new SeasonKey(season.trim().toUpperCase(Locale.ROOT), year);
    }

    /** The season a date falls in. */
    public static SeasonKey of(LocalDate date) {
        return new SeasonKey(SEASONS.get((date.getMonthValue() - 1) / 3), date.getYear());
    }

    private int index() {
        return SEASONS.indexOf(season);
    }

    public SeasonKey next() {
        int i = index();
        return i == 3 ? new SeasonKey(SEASONS.get(0), year + 1) : new SeasonKey(SEASONS.get(i + 1), year);
    }

    public SeasonKey previous() {
        int i = index();
        return i == 0 ? new SeasonKey(SEASONS.get(3), year - 1) : new SeasonKey(SEASONS.get(i - 1), year);
    }

    /** First day of the season (Jan 1, Apr 1, Jul 1 or Oct 1). */
    public LocalDate startDate() {
        return LocalDate.of(year, index() * 3 + 1, 1);
    }

    /** Key used for the season_cache table, e.g. "FALL-2026". */
    public String cacheKey() {
        return season + "-" + year;
    }

    @Override
    public int compareTo(SeasonKey other) {
        return year != other.year ? Integer.compare(year, other.year) : Integer.compare(index(), other.index());
    }
}
