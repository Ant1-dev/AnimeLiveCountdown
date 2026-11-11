package com.Toine.animeCountdownBackend.services;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Works out which instants fall on a given weekday in the viewer's timezone.
 *
 * The weekly schedule groups shows by the day they air for the person looking at it,
 * so "Saturday 11pm in New York" belongs under Saturday even though it is Sunday in UTC.
 * Rather than storing a day per timezone, the schedule query asks for shows whose
 * next airing time falls inside one of the [start, end) windows returned here.
 */
public final class AiringDayWindows {

    private AiringDayWindows() {
    }

    /** A half-open time range: start inclusive, end exclusive. */
    public record Window(Instant start, Instant end) {
        public boolean contains(Instant instant) {
            return !instant.isBefore(start) && instant.isBefore(end);
        }
    }

    /**
     * Parses a client-supplied timezone (IANA name like "America/New_York" or an offset like "-05:00").
     * Falls back to UTC when the value is missing or not a valid zone.
     */
    public static ZoneId resolveZone(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timeZone.trim());
        } catch (DateTimeException e) {
            return ZoneOffset.UTC;
        }
    }

    /**
     * Returns every local day that is {@code day} in {@code zone} and overlaps [from, to],
     * as UTC instant windows. Day boundaries come from the zone's rules, so DST days that
     * are 23 or 25 hours long are handled.
     */
    public static List<Window> forDay(DayOfWeek day, ZoneId zone, Instant from, Instant to) {
        List<Window> windows = new ArrayList<>();
        LocalDate date = from.atZone(zone).toLocalDate();
        LocalDate lastDate = to.atZone(zone).toLocalDate();

        while (date.getDayOfWeek() != day) {
            date = date.plusDays(1);
        }
        for (; !date.isAfter(lastDate); date = date.plusWeeks(1)) {
            windows.add(new Window(
                    date.atStartOfDay(zone).toInstant(),
                    date.plusDays(1).atStartOfDay(zone).toInstant()));
        }
        return windows;
    }
}
