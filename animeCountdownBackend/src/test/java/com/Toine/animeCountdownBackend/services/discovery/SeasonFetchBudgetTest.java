package com.Toine.animeCountdownBackend.services.discovery;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class SeasonFetchBudgetTest {

    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-11T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void allowsUpToTheLimitPerWindow() {
        MutableClock clock = new MutableClock();
        SeasonFetchBudget budget = new SeasonFetchBudget(2, Duration.ofMinutes(10), clock);

        assertTrue(budget.tryAcquire());
        assertTrue(budget.tryAcquire());
        assertFalse(budget.tryAcquire());

        clock.now = clock.now.plus(Duration.ofMinutes(10));
        assertTrue(budget.tryAcquire());
    }
}
