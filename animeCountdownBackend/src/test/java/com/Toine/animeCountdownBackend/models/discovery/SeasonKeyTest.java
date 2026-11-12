package com.Toine.animeCountdownBackend.models.discovery;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SeasonKeyTest {

    @Test
    void mapsMonthsToSeasons() {
        assertEquals(new SeasonKey("WINTER", 2026), SeasonKey.of(LocalDate.of(2026, 1, 1)));
        assertEquals(new SeasonKey("WINTER", 2026), SeasonKey.of(LocalDate.of(2026, 3, 31)));
        assertEquals(new SeasonKey("SPRING", 2026), SeasonKey.of(LocalDate.of(2026, 4, 1)));
        assertEquals(new SeasonKey("SUMMER", 2026), SeasonKey.of(LocalDate.of(2026, 8, 15)));
        assertEquals(new SeasonKey("FALL", 2026), SeasonKey.of(LocalDate.of(2026, 11, 11)));
        assertEquals(new SeasonKey("FALL", 2026), SeasonKey.of(LocalDate.of(2026, 12, 31)));
    }

    @Test
    void nextAndPreviousWrapAcrossYears() {
        assertEquals(new SeasonKey("WINTER", 2027), new SeasonKey("FALL", 2026).next());
        assertEquals(new SeasonKey("FALL", 2025), new SeasonKey("WINTER", 2026).previous());
        assertEquals(new SeasonKey("SUMMER", 2026), new SeasonKey("SPRING", 2026).next());
    }

    @Test
    void startDateIsFirstDayOfPremiereMonth() {
        assertEquals(LocalDate.of(2027, 1, 1), new SeasonKey("WINTER", 2027).startDate());
        assertEquals(LocalDate.of(2026, 10, 1), new SeasonKey("FALL", 2026).startDate());
    }

    @Test
    void parseNormalizesAndValidates() {
        assertEquals(new SeasonKey("SPRING", 2020), SeasonKey.parse(" spring ", 2020));
        assertThrows(IllegalArgumentException.class, () -> SeasonKey.parse("AUTUMN", 2020));
        assertThrows(IllegalArgumentException.class, () -> SeasonKey.parse("FALL", 1900));
        assertThrows(IllegalArgumentException.class, () -> SeasonKey.parse(null, 2020));
    }

    @Test
    void ordersChronologically() {
        assertTrue(new SeasonKey("FALL", 2025).compareTo(new SeasonKey("WINTER", 2026)) < 0);
        assertTrue(new SeasonKey("SUMMER", 2026).compareTo(new SeasonKey("SPRING", 2026)) > 0);
        assertEquals("FALL-2026", new SeasonKey("FALL", 2026).cacheKey());
    }
}
