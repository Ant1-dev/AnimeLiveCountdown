package com.Toine.animeCountdownBackend.services.discovery;

import com.Toine.animeCountdownBackend.components.AniListRateLimiter;
import com.Toine.animeCountdownBackend.models.discovery.SeasonKey;
import com.Toine.animeCountdownBackend.models.discovery.SeasonMedia;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/** Fetches one season's shows from AniList, most popular first. Every request is rate limited. */
@Component
public class AniListSeasonClient {
    static final int PER_PAGE = 50;

    private static final String SEASON_QUERY = """
            query ($page: Int, $season: MediaSeason, $year: Int) {
              Page(page: $page, perPage: 50) {
                media(season: $season, seasonYear: $year, type: ANIME, isAdult: false,
                      format_not: MUSIC, sort: POPULARITY_DESC) {
                  id
                  title { english romaji }
                  coverImage { extraLarge }
                  genres
                  averageScore
                  popularity
                  episodes
                  status
                  format
                  nextAiringEpisode { episode airingAt }
                }
              }
            }
            """;

    private final HttpGraphQlClient graphQlClient;
    private final AniListRateLimiter rateLimiter;
    private final int maxPages;

    public AniListSeasonClient(HttpGraphQlClient graphQlClient, AniListRateLimiter rateLimiter,
                               @Value("${app.seasons.max-pages:2}") int maxPages) {
        this.graphQlClient = graphQlClient;
        this.rateLimiter = rateLimiter;
        this.maxPages = Math.max(1, maxPages);
    }

    /** Up to maxPages requests (50 shows each); stops early on a short page. */
    public Mono<List<SeasonMedia>> fetchSeason(SeasonKey key) {
        return Flux.range(1, maxPages)
                .concatMap(page -> rateLimiter.throttle(graphQlClient.document(SEASON_QUERY)
                        .variable("page", page)
                        .variable("season", key.season())
                        .variable("year", key.year())
                        .retrieve("Page.media")
                        .toEntityList(AniListSeasonMedia.class)))
                .takeUntil(list -> list.size() < PER_PAGE)
                .flatMapIterable(list -> list)
                .map(AniListSeasonMedia::toSeasonMedia)
                .collectList();
    }

    record Title(String english, String romaji) {}
    record Cover(String extraLarge) {}
    record NextEpisode(Integer episode, Long airingAt) {}

    record AniListSeasonMedia(Long id, Title title, Cover coverImage, List<String> genres, Integer averageScore,
                              Long popularity, Integer episodes, String status, String format,
                              NextEpisode nextAiringEpisode) {
        SeasonMedia toSeasonMedia() {
            Integer nextEpisode = null;
            Instant airingAt = null;
            if (nextAiringEpisode != null && nextAiringEpisode.airingAt() != null) {
                nextEpisode = nextAiringEpisode.episode();
                airingAt = Instant.ofEpochSecond(nextAiringEpisode.airingAt());
            }
            return new SeasonMedia(id,
                    title == null ? null : title.english(),
                    title == null ? null : title.romaji(),
                    coverImage == null ? null : coverImage.extraLarge(),
                    genres == null ? List.of() : genres,
                    averageScore, popularity, episodes, status, format, nextEpisode, airingAt);
        }
    }
}
