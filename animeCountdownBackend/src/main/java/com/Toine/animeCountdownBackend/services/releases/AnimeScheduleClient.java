package com.Toine.animeCountdownBackend.services.releases;

import com.Toine.animeCountdownBackend.components.AniListRateLimiter;
import com.Toine.animeCountdownBackend.services.releases.AnimeScheduleModels.AnimeDetails;
import com.Toine.animeCountdownBackend.services.releases.AnimeScheduleModels.TimetableEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

/**
 * AnimeSchedule.net API v3 (https://animeschedule.net/api/v3/documentation) with an application token
 * (ANIMESCHEDULE_TOKEN). Requests are paced by their own limiter, separate from AniList's.
 */
@Component
public class AnimeScheduleClient {
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final String token;
    private final WebClient webClient;
    private final AniListRateLimiter pacer =
            new AniListRateLimiter(Duration.ofMillis(1200), Duration.ofSeconds(61), 1);

    public AnimeScheduleClient(@Value("${app.animeschedule.token:}") String token,
                               @Value("${app.animeschedule.base-url:https://animeschedule.net/api/v3}") String baseUrl) {
        this.token = token == null ? "" : token.trim();
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
                .build();
    }

    /** False until ANIMESCHEDULE_TOKEN is set; the feature stays hidden. */
    public boolean isEnabled() {
        return !token.isEmpty();
    }

    /** One week's timetable for an air type (sub/dub/raw); null year/week means the current week. */
    public List<TimetableEntry> timetable(String airType, Integer year, Integer week) {
        List<TimetableEntry> entries = pacer.throttle(webClient.get()
                        .uri(b -> {
                            b.path("/timetables/{airType}");
                            if (year != null && week != null) {
                                b.queryParam("year", year).queryParam("week", week);
                            }
                            return b.queryParam("tz", "UTC").build(airType);
                        })
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<TimetableEntry>>() { }))
                .block(TIMEOUT);
        return entries == null ? List.of() : entries;
    }

    public AnimeDetails anime(String route) {
        return pacer.throttle(webClient.get()
                        .uri("/anime/{route}", route)
                        .retrieve()
                        .bodyToMono(AnimeDetails.class))
                .block(TIMEOUT);
    }
}
