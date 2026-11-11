package com.Toine.animeCountdownBackend.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Paces every AniList request made by the app and retries ones that were rate limited.
 *
 * AniList allows about 30 requests a minute and answers 429 when a client goes over. All sync
 * jobs share this one instance, so requests are spaced out across jobs too (by default one every
 * 2.1 s, about 28 a minute). A request that still gets a 429 is retried after AniList's lockout
 * window instead of failing the whole sync.
 */
@Component
public class AniListRateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(AniListRateLimiter.class);

    private final long spacingNanos;
    private final Duration rateLimitBackoff;
    private final int maxRetries;

    // Earliest System.nanoTime() at which the next request may start
    private long nextSlotNanos = System.nanoTime();

    @Autowired
    public AniListRateLimiter(@Value("${app.anilist.min-request-spacing-ms:2100}") long spacingMs) {
        this(Duration.ofMillis(spacingMs), Duration.ofSeconds(61), 2);
    }

    AniListRateLimiter(Duration spacing, Duration rateLimitBackoff, int maxRetries) {
        this.spacingNanos = spacing.toNanos();
        this.rateLimitBackoff = rateLimitBackoff;
        this.maxRetries = maxRetries;
    }

    /**
     * Runs the request once its turn comes up, retrying after a pause if AniList rate limits it.
     * The request must be cold (re-subscribing sends it again), which GraphQL client calls are.
     */
    public <T> Mono<T> throttle(Mono<T> request) {
        return Mono.defer(() -> Mono.delay(reserveSlot()).then(request))
                .retryWhen(Retry.fixedDelay(maxRetries, rateLimitBackoff)
                        .filter(AniListRateLimiter::isRateLimited)
                        .doBeforeRetry(signal -> logger.warn(
                                "AniList rate limit hit; retrying in {}s (attempt {} of {})",
                                rateLimitBackoff.toSeconds(), signal.totalRetries() + 1, maxRetries))
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
    }

    private synchronized Duration reserveSlot() {
        long now = System.nanoTime();
        long start = Math.max(now, nextSlotNanos);
        nextSlotNanos = start + spacingNanos;
        return Duration.ofNanos(start - now);
    }

    /** True if the error, or anything that caused it, is an HTTP 429 from AniList. */
    public static boolean isRateLimited(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof WebClientResponseException w && w.getStatusCode().value() == 429) {
                return true;
            }
            if (t.getMessage() != null && t.getMessage().contains("429")) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }
}
