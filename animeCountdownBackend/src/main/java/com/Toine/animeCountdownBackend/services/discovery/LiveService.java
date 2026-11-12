package com.Toine.animeCountdownBackend.services.discovery;

import com.Toine.animeCountdownBackend.components.AniListRateLimiter;
import com.Toine.animeCountdownBackend.models.discovery.LiveEpisode;
import com.Toine.animeCountdownBackend.models.discovery.LiveResponse;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * "Live moments" on the home page: episodes that aired in the last hour (some are still airing)
 * and episodes airing in the next two hours.
 *
 * The media table only remembers each show's next episode, which the just-aired sync moves forward
 * a minute after airing, so "airing now" needs AniList's airing schedule. One request covers the
 * whole window and is cached for {@code app.live.cache-ttl} (2 minutes), so this costs at most one
 * AniList request every two minutes however many people are on the site. Only shows the site
 * tracks (rows in media) are returned. If AniList fails we fall back to upcoming episodes from the
 * media table.
 */
@Service
public class LiveService {
    private static final Logger logger = LoggerFactory.getLogger(LiveService.class);
    static final Duration LOOK_BACK = Duration.ofHours(1);
    static final Duration LOOK_AHEAD = Duration.ofHours(2);
    private static final int DEFAULT_DURATION_MINUTES = 24;

    private static final String QUERY = """
            query ($from: Int, $to: Int) {
              Page(page: 1, perPage: 50) {
                airingSchedules(airingAt_greater: $from, airingAt_lesser: $to, sort: TIME) {
                  episode
                  airingAt
                  media {
                    id
                    isAdult
                    duration
                    title { english romaji }
                    coverImage { large }
                  }
                }
              }
            }
            """;

    private final HttpGraphQlClient graphQlClient;
    private final AniListRateLimiter rateLimiter;
    private final MediaRepository mediaRepository;
    private final Duration cacheTtl;
    private final Clock clock;

    private volatile LiveResponse cached;

    @Autowired
    public LiveService(HttpGraphQlClient graphQlClient, AniListRateLimiter rateLimiter, MediaRepository mediaRepository,
                       @Value("${app.live.cache-ttl:PT2M}") Duration cacheTtl) {
        this(graphQlClient, rateLimiter, mediaRepository, cacheTtl, Clock.systemUTC());
    }

    LiveService(HttpGraphQlClient graphQlClient, AniListRateLimiter rateLimiter, MediaRepository mediaRepository,
                Duration cacheTtl, Clock clock) {
        this.graphQlClient = graphQlClient;
        this.rateLimiter = rateLimiter;
        this.mediaRepository = mediaRepository;
        this.cacheTtl = cacheTtl;
        this.clock = clock;
    }

    public LiveResponse getLive() {
        LiveResponse current = cached;
        if (current != null && current.generatedAt().plus(cacheTtl).isAfter(clock.instant())) {
            return current;
        }
        synchronized (this) {
            current = cached;
            if (current != null && current.generatedAt().plus(cacheTtl).isAfter(clock.instant())) {
                return current;
            }
            cached = refresh();
            return cached;
        }
    }

    private LiveResponse refresh() {
        Instant now = clock.instant();
        Instant from = now.minus(LOOK_BACK);
        Instant to = now.plus(LOOK_AHEAD);
        try {
            List<Schedule> schedules = rateLimiter.throttle(graphQlClient.document(QUERY)
                            .variable("from", from.getEpochSecond())
                            .variable("to", to.getEpochSecond())
                            .retrieve("Page.airingSchedules")
                            .toEntityList(Schedule.class))
                    .block(Duration.ofSeconds(30));
            return new LiveResponse(now, from, to, toEpisodes(schedules, trackedIds(schedules)));
        } catch (RuntimeException e) {
            logger.warn("AniList airing schedule failed, using the media table: {}", e.getMessage());
            return new LiveResponse(now, now, to, fromMediaTable(now, to));
        }
    }

    private Set<Long> trackedIds(List<Schedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return Set.of();
        }
        Set<Long> ids = schedules.stream()
                .filter(s -> s.media() != null && s.media().id() != null)
                .map(s -> s.media().id())
                .collect(Collectors.toSet());
        Set<Long> tracked = new HashSet<>();
        mediaRepository.findAllById(ids).forEach(m -> tracked.add(m.getId()));
        return tracked;
    }

    static List<LiveEpisode> toEpisodes(List<Schedule> schedules, Set<Long> trackedIds) {
        if (schedules == null) {
            return List.of();
        }
        return schedules.stream()
                .filter(s -> s.media() != null && s.airingAt() != null)
                .filter(s -> !Boolean.TRUE.equals(s.media().isAdult()))
                .filter(s -> trackedIds.contains(s.media().id()))
                .map(s -> new LiveEpisode(
                        s.media().id(),
                        s.media().title() == null ? null : s.media().title().english(),
                        s.media().title() == null ? null : s.media().title().romaji(),
                        s.media().coverImage() == null ? null : s.media().coverImage().large(),
                        s.episode(),
                        Instant.ofEpochSecond(s.airingAt()),
                        s.media().duration() == null || s.media().duration() <= 0
                                ? DEFAULT_DURATION_MINUTES : s.media().duration()))
                .sorted(Comparator.comparing(LiveEpisode::airingAt))
                .toList();
    }

    private List<LiveEpisode> fromMediaTable(Instant now, Instant to) {
        try {
            return mediaRepository.findUpcomingEpisodes(now, PageRequest.of(0, 50)).getContent().stream()
                    .filter(m -> m.getNext_Airing_At() != null && !m.getNext_Airing_At().isAfter(to))
                    .map(LiveService::fromMedia)
                    .toList();
        } catch (RuntimeException e) {
            logger.warn("Couldn't read upcoming episodes: {}", e.getMessage());
            return List.of();
        }
    }

    private static LiveEpisode fromMedia(MediaEntity m) {
        return new LiveEpisode(m.getId(), m.getTitle_English(), m.getTitle_Romaji(), m.getCover_Image_Url(),
                m.getNext_Airing_Episode(), m.getNext_Airing_At(), DEFAULT_DURATION_MINUTES);
    }

    record Title(String english, String romaji) {}
    record Cover(String large) {}
    record ScheduleMedia(Long id, Boolean isAdult, Integer duration, Title title, Cover coverImage) {}
    record Schedule(Integer episode, Long airingAt, ScheduleMedia media) {}
}
