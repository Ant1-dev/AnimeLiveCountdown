package com.Toine.animeCountdownBackend.components;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AniListRateLimiterTest {

    @Test
    void spacesRequestsApart() {
        AniListRateLimiter limiter = new AniListRateLimiter(Duration.ofMillis(100), Duration.ofMillis(10), 2);

        long start = System.nanoTime();
        List<Integer> results = Flux.range(1, 3)
                .concatMap(i -> limiter.throttle(Mono.just(i)))
                .collectList()
                .block();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertEquals(List.of(1, 2, 3), results);
        assertTrue(elapsedMs >= 190, "3 requests at 100 ms spacing should take ~200 ms, took " + elapsedMs);
    }

    @Test
    void retriesAfterRateLimitThenSucceeds() {
        AniListRateLimiter limiter = new AniListRateLimiter(Duration.ZERO, Duration.ofMillis(10), 2);
        AtomicInteger attempts = new AtomicInteger();

        String result = limiter.throttle(Mono.defer(() ->
                attempts.incrementAndGet() == 1 ? Mono.error(tooManyRequests()) : Mono.just("ok"))).block();

        assertEquals("ok", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void givesUpAfterMaxRetries() {
        AniListRateLimiter limiter = new AniListRateLimiter(Duration.ZERO, Duration.ofMillis(10), 2);
        AtomicInteger attempts = new AtomicInteger();

        Mono<String> alwaysLimited = Mono.defer(() -> {
            attempts.incrementAndGet();
            return Mono.error(tooManyRequests());
        });

        RuntimeException error = assertThrows(RuntimeException.class, () -> limiter.throttle(alwaysLimited).block());
        assertTrue(AniListRateLimiter.isRateLimited(error));
        assertEquals(3, attempts.get(), "first try plus 2 retries");
    }

    @Test
    void doesNotRetryOtherErrors() {
        AniListRateLimiter limiter = new AniListRateLimiter(Duration.ZERO, Duration.ofMillis(10), 2);
        AtomicInteger attempts = new AtomicInteger();

        Mono<String> failing = Mono.defer(() -> {
            attempts.incrementAndGet();
            return Mono.error(new IllegalStateException("boom"));
        });

        assertThrows(IllegalStateException.class, () -> limiter.throttle(failing).block());
        assertEquals(1, attempts.get());
    }

    @Test
    void detectsRateLimitWrappedInOtherExceptions() {
        assertTrue(AniListRateLimiter.isRateLimited(new RuntimeException("transport failed", tooManyRequests())));
        assertFalse(AniListRateLimiter.isRateLimited(new RuntimeException("connection reset")));
    }

    private static WebClientResponseException tooManyRequests() {
        return WebClientResponseException.create(429, "Too Many Requests", HttpHeaders.EMPTY, new byte[0], null);
    }
}
