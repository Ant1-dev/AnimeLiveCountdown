package com.Toine.animeCountdownBackend.services.discovery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Caps how many uncached seasons can be fetched from AniList per time window. The sync jobs share
 * AniList's ~30 requests/min with the season browser, so a visitor clicking through twenty years of
 * seasons must not push the schedule sync back by minutes.
 */
@Component
public class SeasonFetchBudget {
    private final int maxFetches;
    private final Duration window;
    private final Clock clock;
    private final Deque<Instant> recent = new ArrayDeque<>();

    @Autowired
    public SeasonFetchBudget(@Value("${app.seasons.max-fetches-per-window:6}") int maxFetches,
                             @Value("${app.seasons.fetch-window:PT10M}") Duration window) {
        this(maxFetches, window, Clock.systemUTC());
    }

    SeasonFetchBudget(int maxFetches, Duration window, Clock clock) {
        this.maxFetches = maxFetches;
        this.window = window;
        this.clock = clock;
    }

    /** Takes one fetch from the budget; false when the window is used up. */
    public synchronized boolean tryAcquire() {
        Instant now = clock.instant();
        while (!recent.isEmpty() && !recent.peekFirst().isAfter(now.minus(window))) {
            recent.pollFirst();
        }
        if (recent.size() >= maxFetches) {
            return false;
        }
        recent.addLast(now);
        return true;
    }
}
