package com.Toine.animeCountdownBackend.services;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiringDayWindowsTest {

    private static final ZoneId EASTERN = ZoneId.of("America/New_York");

    // Saturday 2026-11-14 at 11pm Eastern is Sunday 2026-11-15 04:00 in UTC.
    private static final Instant SATURDAY_11PM_EASTERN =
            LocalDateTime.of(2026, 11, 14, 23, 0).atZone(EASTERN).toInstant();

    private static final Instant FROM = Instant.parse("2026-11-04T12:00:00Z");
    private static final Instant TO = Instant.parse("2026-12-01T12:00:00Z");

    private static boolean airsOn(DayOfWeek day, ZoneId zone, Instant airingAt) {
        return AiringDayWindows.forDay(day, zone, FROM, TO).stream().anyMatch(w -> w.contains(airingAt));
    }

    @Test
    void lateEveningEasternShowIsSaturdayForEasternViewers() {
        assertThat(SATURDAY_11PM_EASTERN).isEqualTo(Instant.parse("2026-11-15T04:00:00Z"));

        assertThat(airsOn(DayOfWeek.SATURDAY, EASTERN, SATURDAY_11PM_EASTERN)).isTrue();
        assertThat(airsOn(DayOfWeek.SUNDAY, EASTERN, SATURDAY_11PM_EASTERN)).isFalse();
    }

    @Test
    void sameShowIsSundayForUtcViewers() {
        assertThat(airsOn(DayOfWeek.SUNDAY, ZoneOffset.UTC, SATURDAY_11PM_EASTERN)).isTrue();
        assertThat(airsOn(DayOfWeek.SATURDAY, ZoneOffset.UTC, SATURDAY_11PM_EASTERN)).isFalse();
    }

    @Test
    void windowsAreWholeLocalDaysOneWeekApart() {
        List<AiringDayWindows.Window> windows = AiringDayWindows.forDay(DayOfWeek.SATURDAY, EASTERN, FROM, TO);

        assertThat(windows).containsExactly(
                new AiringDayWindows.Window(Instant.parse("2026-11-07T05:00:00Z"), Instant.parse("2026-11-08T05:00:00Z")),
                new AiringDayWindows.Window(Instant.parse("2026-11-14T05:00:00Z"), Instant.parse("2026-11-15T05:00:00Z")),
                new AiringDayWindows.Window(Instant.parse("2026-11-21T05:00:00Z"), Instant.parse("2026-11-22T05:00:00Z")),
                new AiringDayWindows.Window(Instant.parse("2026-11-28T05:00:00Z"), Instant.parse("2026-11-29T05:00:00Z")));
    }

    @Test
    void dstChangeDayIsTwentyFiveHoursLong() {
        // US clocks fell back on Sunday 2026-11-01.
        List<AiringDayWindows.Window> windows = AiringDayWindows.forDay(DayOfWeek.SUNDAY, EASTERN,
                Instant.parse("2026-10-31T12:00:00Z"), Instant.parse("2026-11-02T12:00:00Z"));

        assertThat(windows).hasSize(1);
        assertThat(ChronoUnit.HOURS.between(windows.get(0).start(), windows.get(0).end())).isEqualTo(25);
    }

    @Test
    void resolveZoneAcceptsIanaNamesAndOffsets() {
        assertThat(AiringDayWindows.resolveZone("America/New_York")).isEqualTo(EASTERN);
        assertThat(AiringDayWindows.resolveZone("-05:00")).isEqualTo(ZoneOffset.ofHours(-5));
    }

    @Test
    void resolveZoneFallsBackToUtc() {
        assertThat(AiringDayWindows.resolveZone(null)).isEqualTo(ZoneOffset.UTC);
        assertThat(AiringDayWindows.resolveZone("")).isEqualTo(ZoneOffset.UTC);
        assertThat(AiringDayWindows.resolveZone("Not/AZone")).isEqualTo(ZoneOffset.UTC);
        assertThat(AiringDayWindows.resolveZone("garbage!!")).isEqualTo(ZoneOffset.UTC);
    }
}
