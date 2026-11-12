package com.Toine.animeCountdownBackend.services.discovery;

import com.Toine.animeCountdownBackend.models.discovery.*;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.SeasonCacheEntity;
import com.Toine.animeCountdownBackend.repositories.SeasonCacheRepository;
import com.Toine.animeCountdownBackend.repositories.SeasonMediaInfoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Season browser data.
 *
 * The current season and anything later come from media_info, which the media_info sync already
 * keeps fresh (no extra AniList requests). Past seasons are fetched from AniList the first time
 * someone asks, then cached in season_cache for {@code app.seasons.cache-ttl} (7 days by default).
 * Concurrent requests for the same season share one fetch, and {@link SeasonFetchBudget} caps
 * uncached fetches so browsing can't starve the schedule sync of AniList requests.
 */
@Service
public class SeasonService {
    private static final Logger logger = LoggerFactory.getLogger(SeasonService.class);
    private static final TypeReference<List<SeasonMedia>> MEDIA_LIST = new TypeReference<>() {};
    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(90);

    private final SeasonMediaInfoRepository mediaInfoRepository;
    private final SeasonCacheRepository cacheRepository;
    private final AniListSeasonClient aniListClient;
    private final SeasonFetchBudget budget;
    private final ObjectMapper objectMapper;
    private final Duration cacheTtl;
    private final Clock clock;
    private final ConcurrentHashMap<String, CompletableFuture<List<SeasonMedia>>> inFlight = new ConcurrentHashMap<>();

    @Autowired
    public SeasonService(SeasonMediaInfoRepository mediaInfoRepository, SeasonCacheRepository cacheRepository,
                         AniListSeasonClient aniListClient, SeasonFetchBudget budget, ObjectMapper objectMapper,
                         @Value("${app.seasons.cache-ttl:P7D}") Duration cacheTtl) {
        this(mediaInfoRepository, cacheRepository, aniListClient, budget, objectMapper, cacheTtl, Clock.systemUTC());
    }

    SeasonService(SeasonMediaInfoRepository mediaInfoRepository, SeasonCacheRepository cacheRepository,
                  AniListSeasonClient aniListClient, SeasonFetchBudget budget, ObjectMapper objectMapper,
                  Duration cacheTtl, Clock clock) {
        this.mediaInfoRepository = mediaInfoRepository;
        this.cacheRepository = cacheRepository;
        this.aniListClient = aniListClient;
        this.budget = budget;
        this.objectMapper = objectMapper;
        this.cacheTtl = cacheTtl;
        this.clock = clock;
    }

    public SeasonKey currentSeason() {
        return SeasonKey.of(LocalDate.now(clock.withZone(ZoneOffset.UTC)));
    }

    public SeasonOverview overview() {
        SeasonKey current = currentSeason();
        SeasonKey next = current.next();
        return new SeasonOverview(current, next, next.startDate(), SeasonKey.MIN_YEAR);
    }

    // Deliberately not @Transactional: a past-season fetch can wait on AniList for a while and
    // shouldn't hold a DB connection meanwhile. The local query fetch-joins genres.
    public SeasonResponse getSeason(SeasonKey key, String genre, SeasonSort sort) {
        boolean local = key.compareTo(currentSeason()) >= 0;
        List<SeasonMedia> all = local ? loadLocal(key) : loadPast(key);
        List<SeasonMedia> media = SeasonFilter.apply(all, genre, sort, clock.instant());
        return new SeasonResponse(key.season(), key.year(), key.startDate(), local ? "local" : "anilist",
                SeasonFilter.genres(all), media);
    }

    private List<SeasonMedia> loadLocal(SeasonKey key) {
        return mediaInfoRepository.findBySeason(key.season(), key.year()).stream()
                .map(SeasonService::fromEntity)
                .toList();
    }

    private List<SeasonMedia> loadPast(SeasonKey key) {
        Optional<SeasonCacheEntity> cached = cacheRepository.findById(key.cacheKey());
        Optional<List<SeasonMedia>> cachedMedia = cached.flatMap(this::readPayload);
        boolean fresh = cached.isPresent() && cachedMedia.isPresent()
                && cached.get().getFetchedAt().plus(cacheTtl).isAfter(clock.instant());
        if (fresh) {
            return cachedMedia.get();
        }
        try {
            return fetchShared(key);
        } catch (SeasonUnavailableException e) {
            if (cachedMedia.isPresent()) {
                logger.info("Serving stale cache for {}: {}", key.cacheKey(), e.getMessage());
                return cachedMedia.get();
            }
            throw e;
        }
    }

    /** One AniList fetch per season at a time; other callers wait for the same result. */
    private List<SeasonMedia> fetchShared(SeasonKey key) {
        CompletableFuture<List<SeasonMedia>> mine = new CompletableFuture<>();
        CompletableFuture<List<SeasonMedia>> existing = inFlight.putIfAbsent(key.cacheKey(), mine);
        if (existing != null) {
            return await(existing, key);
        }
        try {
            if (!budget.tryAcquire()) {
                throw new SeasonUnavailableException("Too many seasons requested right now; try again in a few minutes");
            }
            List<SeasonMedia> media = fetchAndStore(key);
            mine.complete(media);
            return media;
        } catch (RuntimeException e) {
            mine.completeExceptionally(e);
            throw e;
        } finally {
            inFlight.remove(key.cacheKey(), mine);
        }
    }

    private List<SeasonMedia> fetchAndStore(SeasonKey key) {
        List<SeasonMedia> media;
        try {
            media = aniListClient.fetchSeason(key).block(FETCH_TIMEOUT);
        } catch (RuntimeException e) {
            logger.warn("AniList season fetch failed for {}: {}", key.cacheKey(), e.getMessage());
            throw new SeasonUnavailableException("Couldn't load this season from AniList; try again shortly", e);
        }
        if (media == null) {
            media = List.of();
        }
        try {
            cacheRepository.save(new SeasonCacheEntity(key.cacheKey(), objectMapper.writeValueAsString(media), clock.instant()));
        } catch (JsonProcessingException | RuntimeException e) {
            // Still answer the request; we'll just fetch again next time.
            logger.warn("Couldn't cache season {}: {}", key.cacheKey(), e.getMessage());
        }
        logger.info("Fetched {} shows for {} from AniList", media.size(), key.cacheKey());
        return media;
    }

    private static List<SeasonMedia> await(CompletableFuture<List<SeasonMedia>> future, SeasonKey key) {
        try {
            return future.get(FETCH_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof SeasonUnavailableException unavailable) {
                throw unavailable;
            }
            throw new SeasonUnavailableException("Couldn't load " + key.cacheKey(), e.getCause());
        } catch (TimeoutException e) {
            throw new SeasonUnavailableException("Timed out loading " + key.cacheKey(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SeasonUnavailableException("Interrupted loading " + key.cacheKey(), e);
        }
    }

    private Optional<List<SeasonMedia>> readPayload(SeasonCacheEntity entity) {
        try {
            return Optional.of(objectMapper.readValue(entity.getPayload(), MEDIA_LIST));
        } catch (JsonProcessingException e) {
            logger.warn("Ignoring unreadable season cache for {}", entity.getSeasonKey());
            return Optional.empty();
        }
    }

    static SeasonMedia fromEntity(MediaInfoEntity e) {
        return new SeasonMedia(e.getId(), e.getEngtitle(), e.getRomtitle(), e.getCoverimage(),
                e.getGenres() == null ? List.of() : List.copyOf(e.getGenres()),
                e.getAvgscore(), e.getPopularity(), e.getTotalepisodes(), e.getStatus(), null,
                e.getNextepisode(), e.getAiringat());
    }
}
