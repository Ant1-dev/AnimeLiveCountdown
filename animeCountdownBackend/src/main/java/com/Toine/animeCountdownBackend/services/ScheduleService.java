package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.components.AniListRateLimiter;
import com.Toine.animeCountdownBackend.models.graphQlMedia.Media;
import com.Toine.animeCountdownBackend.models.graphQlMedia.Page;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.repositories.FavoriteMediaRepository;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class ScheduleService {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

    private static final String FINISHED_STATUS = "FINISHED";

    // Only look this far back for aired episodes; anything older is left to the sweeps, so a show
    // AniList hasn't updated yet isn't re-fetched every minute forever.
    private static final Duration JUST_AIRED_LOOKBACK = Duration.ofHours(6);
    private static final int ID_BATCH_SIZE = 50;

    /** Which stored shows a full sweep owns, and so may remove when they're missing from AniList. */
    public enum SyncScope {
        /** RELEASING sweep; owns every stored show that isn't NOT_YET_RELEASED. */
        AIRING("RELEASING"),
        /** NOT_YET_RELEASED sweep; owns only NOT_YET_RELEASED shows. */
        UPCOMING("NOT_YET_RELEASED");

        final String anilistStatus;

        SyncScope(String anilistStatus) {
            this.anilistStatus = anilistStatus;
        }
    }

    private final MediaRepository mediaRepository;
    private final FavoriteMediaRepository favoriteMediaRepository;
    private final HttpGraphQlClient graphQlClient;
    private final AniListRateLimiter aniListRateLimiter;

    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);

    // Calls to updateDatabase must go through the Spring proxy, otherwise @Transactional and
    // @CacheEvict are skipped (self-invocation). @Lazy avoids a circular-reference error at startup.
    @Autowired
    @Lazy
    private ScheduleService self;

    public ScheduleService(HttpGraphQlClient graphQlClient, MediaRepository mediaRepository,
                           FavoriteMediaRepository favoriteMediaRepository, AniListRateLimiter aniListRateLimiter) {
        this.graphQlClient = graphQlClient;
        this.aniListRateLimiter = aniListRateLimiter;
        this.mediaRepository = mediaRepository;
        this.favoriteMediaRepository = favoriteMediaRepository;
    }

    /*
     * AniList sync, split by how fast each kind of data changes:
     *
     * - Just aired (every minute): re-fetches only shows whose stored next episode time has passed,
     *   so the card moves on to the next episode's countdown within about a minute. Usually 0-1
     *   requests. Updates only; never removes shows.
     * - Airing sweep (every 30 min): all RELEASING shows. Picks up delays, reschedules and shows that
     *   started airing, and removes shows that stopped airing.
     * - Upcoming sweep (every 6 h): all NOT_YET_RELEASED shows. Premiere dates rarely change.
     *
     * Countdowns are computed in the browser from next_Airing_At, so they stay live between syncs.
     * The jobs share one lock so they never write at the same time.
     */

    @Scheduled(initialDelayString = "${app.sync.just-aired.initial-delay:PT1M}",
               fixedDelayString = "${app.sync.just-aired.interval:PT1M}")
    public void syncJustAired() {
        runExclusively("just-aired", () -> {
            Instant now = Instant.now();
            List<Long> ids = mediaRepository.findIdsAiringBetween(now.minus(JUST_AIRED_LOOKBACK), now);
            if (ids.isEmpty()) {
                return;
            }
            List<MediaEntity> fresh = fetchByIds(ids).block();
            if (fresh == null || fresh.isEmpty()) {
                return;
            }
            int updated = self.applyUpdates(fresh);
            logger.info("Just-aired sync: refreshed {} of {} shows whose episode aired", updated, ids.size());
        });
    }

    @Scheduled(initialDelayString = "${app.sync.airing.initial-delay:PT0S}",
               fixedDelayString = "${app.sync.airing.interval:PT30M}")
    public void syncAiring() {
        runExclusively("airing sweep", () -> sweep(SyncScope.AIRING));
    }

    @Scheduled(initialDelayString = "${app.sync.upcoming.initial-delay:PT2M}",
               fixedDelayString = "${app.sync.upcoming.interval:PT6H}")
    public void syncUpcoming() {
        runExclusively("upcoming sweep", () -> sweep(SyncScope.UPCOMING));
    }

    private void sweep(SyncScope scope) {
        List<MediaEntity> fresh = fetchAllWithStatus(scope.anilistStatus).block();
        if (fresh == null || fresh.isEmpty()) {
            // An empty result is far more likely an API problem than every show disappearing,
            // so don't treat it as "remove everything".
            logger.warn("{} sync returned no shows, skipping", scope);
            return;
        }
        logger.info("{} sync fetched {} shows", scope, fresh.size());
        self.updateDatabase(fresh, scope);
    }

    private void runExclusively(String jobName, Runnable job) {
        if (!syncInProgress.compareAndSet(false, true)) {
            logger.info("Skipping {} sync: another sync is still running", jobName);
            return;
        }
        try {
            job.run();
        } catch (org.springframework.graphql.client.GraphQlTransportException e) {
            if (AniListRateLimiter.isRateLimited(e)) {
                logger.warn("AniList kept rate limiting the {} sync after retries; will try again next cycle", jobName);
            } else {
                logger.error("AniList request failed during {} sync: {}", jobName, e.getMessage());
            }
        } catch (RuntimeException e) {
            logger.error("{} sync failed: {}", jobName, e.getMessage(), e);
        } finally {
            syncInProgress.set(false);
        }
    }

    /**
     * Applies fresh AniList data without removing anything. Used by the just-aired job. A show that
     * stopped airing gets its new status here and is hidden by the schedule queries; the next airing
     * sweep then removes it (or keeps it as FINISHED if someone favorited it).
     *
     * @return the number of shows that changed
     */
    @Transactional
    public int applyUpdates(List<MediaEntity> freshEntities) {
        Map<Long, MediaEntity> existing = mediaRepository
                .findAllById(freshEntities.stream().map(MediaEntity::getId).toList())
                .stream()
                .collect(Collectors.toMap(MediaEntity::getId, e -> e));

        List<MediaEntity> changed = new ArrayList<>();
        for (MediaEntity fresh : freshEntities) {
            MediaEntity current = existing.get(fresh.getId());
            if (current != null && hasSignificantChanges(current, fresh)) {
                updateEntityFields(current, fresh);
                changed.add(current);
            }
        }
        if (!changed.isEmpty()) {
            mediaRepository.saveAll(changed);
        }
        return changed.size();
    }

    /**
     * Merges a full sweep into the database. Shows in the sweep are inserted or updated. Stored shows
     * that this sweep is responsible for (see {@link SyncScope}) but that are missing from it are
     * removed, except favorited ones, which are kept and marked FINISHED so users' favorites survive.
     */
    @Transactional
    @CacheEvict(value = {"weekdayAnime", "upcomingAnime", "trendingAnime"}, allEntries = true)
    public void updateDatabase(List<MediaEntity> newEntities, SyncScope scope) {
        Set<Long> newIds = newEntities.stream().map(MediaEntity::getId).collect(Collectors.toSet());

        // Existing rows we may update (any status: an upcoming show can show up in the airing sweep)
        Map<Long, MediaEntity> existingEntitiesMap = new HashMap<>();
        mediaRepository.findAllById(newIds).forEach(e -> existingEntitiesMap.put(e.getId(), e));

        // Existing rows this sweep is allowed to remove
        List<MediaEntity> removalCandidates = scope == SyncScope.AIRING
                ? mediaRepository.findNotUpcoming()
                : mediaRepository.findUpcoming();

        List<MediaEntity> entitiesToUpdate = new ArrayList<>();
        List<MediaEntity> entitiesToInsert = new ArrayList<>();

        for (MediaEntity newEntity : newEntities) {
            MediaEntity existingEntity = existingEntitiesMap.get(newEntity.getId());
            if (existingEntity == null) {
                entitiesToInsert.add(newEntity);
            } else if (hasSignificantChanges(existingEntity, newEntity)) {
                updateEntityFields(existingEntity, newEntity);
                entitiesToUpdate.add(existingEntity);
            }
        }

        if (!entitiesToUpdate.isEmpty()) {
            mediaRepository.saveAll(entitiesToUpdate);
        }
        if (!entitiesToInsert.isEmpty()) {
            mediaRepository.saveAll(entitiesToInsert);
        }

        Map<Long, MediaEntity> missing = removalCandidates.stream()
                .filter(e -> !newIds.contains(e.getId()))
                .collect(Collectors.toMap(MediaEntity::getId, e -> e));

        int retirements = 0;
        int deletions = 0;
        if (!missing.isEmpty()) {
            Set<Long> favoritedIds = favoriteMediaRepository.findFavoritedMediaIds(missing.keySet());

            List<MediaEntity> entitiesToRetire = new ArrayList<>();
            for (Long id : favoritedIds) {
                MediaEntity entity = missing.get(id);
                if (entity != null && !FINISHED_STATUS.equals(entity.getStatus())) {
                    entity.setStatus(FINISHED_STATUS);
                    entity.setNext_Airing_At(null);
                    entity.setNext_Airing_Episode(null);
                    entity.setDay(null);
                    entitiesToRetire.add(entity);
                }
            }
            if (!entitiesToRetire.isEmpty()) {
                mediaRepository.saveAll(entitiesToRetire);
                retirements = entitiesToRetire.size();
            }

            Set<Long> idsToDelete = new HashSet<>(missing.keySet());
            idsToDelete.removeAll(favoritedIds);
            if (!idsToDelete.isEmpty()) {
                mediaRepository.deleteAllById(idsToDelete);
                deletions = idsToDelete.size();
            }
        }

        logger.info("{} sync applied: {} updates, {} insertions, {} deletions, {} favorited shows marked finished",
                scope, entitiesToUpdate.size(), entitiesToInsert.size(), deletions, retirements);
    }

    /**
     * Check if an anime entity has significant changes that require an update
     */
    private boolean hasSignificantChanges(MediaEntity existing, MediaEntity newEntity) {
        if (existing == null || newEntity == null) {
            return true; // Null check to prevent NullPointerException
        }

        return !Objects.equals(existing.getNext_Airing_At(), newEntity.getNext_Airing_At()) ||
               !Objects.equals(existing.getNext_Airing_Episode(), newEntity.getNext_Airing_Episode()) ||
               !Objects.equals(existing.getStatus(), newEntity.getStatus()) ||
               !Objects.equals(existing.getTitle_English(), newEntity.getTitle_English()) ||
               !Objects.equals(existing.getTitle_Romaji(), newEntity.getTitle_Romaji()) ||
               !Objects.equals(existing.getCover_Image_Url(), newEntity.getCover_Image_Url()) ||
               !Objects.equals(existing.getPopularity(), newEntity.getPopularity()) ||
               !Objects.equals(existing.getDay(), newEntity.getDay()) ||
               !Objects.equals(existing.getSeasonYear(), newEntity.getSeasonYear()) ||
               !Objects.equals(existing.getSeason(), newEntity.getSeason()) ||
               !Objects.equals(existing.getBanner_Image_Url(), newEntity.getBanner_Image_Url());
    }

    /**
     * Update the fields of an existing entity with values from a new entity
     */
    private void updateEntityFields(MediaEntity existing, MediaEntity newEntity) {
        // Compare stored vs fresh airing info before it's overwritten
        ScheduleNoteDetector.detect(existing, newEntity, Instant.now());

        // Update basic info
        existing.setTitle_English(newEntity.getTitle_English());
        existing.setTitle_Romaji(newEntity.getTitle_Romaji());
        existing.setStatus(newEntity.getStatus());
        existing.setPopularity(newEntity.getPopularity());
        existing.setCover_Image_Url(newEntity.getCover_Image_Url());
        existing.setSeasonYear(newEntity.getSeasonYear());
        existing.setSeason(newEntity.getSeason());
        existing.setBanner_Image_Url(newEntity.getBanner_Image_Url());

        // Update airing info
        existing.setNext_Airing_At(newEntity.getNext_Airing_At());
        existing.setNext_Airing_Episode(newEntity.getNext_Airing_Episode());
        existing.setDay(newEntity.getDay());
    }

    public MediaEntity convertToEntity(Media media) {
        MediaEntity entity = new MediaEntity();
        entity.setId(media.getId());
        entity.setTitle_Romaji(media.getTitle().getRomaji());
        entity.setTitle_English(media.getTitle().getEnglish());
        entity.setStatus(media.getStatus());
        entity.setPopularity(media.getPopularity());
        entity.setSeasonYear(media.getSeasonYear());
        entity.setSeason(media.getSeason());
        entity.setBanner_Image_Url(media.getBannerImage());

        if (media.getNextAiringEpisode() != null && media.getNextAiringEpisode().getAiringAt() != null) {
            Instant airingTime = Instant.ofEpochSecond(media.getNextAiringEpisode().getAiringAt());
            entity.setNext_Airing_At(airingTime);
            entity.setNext_Airing_Episode(media.getNextAiringEpisode().getEpisode());
            entity.setDay(airingTime.atOffset(java.time.ZoneOffset.UTC).getDayOfWeek().toString());
        } else {
            entity.setNext_Airing_At(null);
            entity.setNext_Airing_Episode(null);
            entity.setDay(null);
        }

        if (media.getCoverImage() != null) {
            entity.setCover_Image_Url(media.getCoverImage().getLarge());
        }

        return entity;
    }

    private static final String MEDIA_FIELDS = """
              id
              popularity
              title {
                english
                romaji
              }
              status
              seasonYear
              season
              bannerImage
              nextAiringEpisode {
                airingAt
                episode
              }
              coverImage {
                large
              }
            """;

    private static final String BY_STATUS_QUERY = """
        query ($page: Int, $status: MediaStatus) {
          Page(page: $page, perPage: 50) {
            media(isAdult: false, type: ANIME, status: $status, sort: POPULARITY_DESC) {
              %s
            }
          }
        }
        """.formatted(MEDIA_FIELDS);

    private static final String BY_IDS_QUERY = """
        query ($ids: [Int]) {
          Page(page: 1, perPage: 50) {
            media(id_in: $ids, type: ANIME) {
              %s
            }
          }
        }
        """.formatted(MEDIA_FIELDS);

    /** Fetches every show with the given AniList status, one page of 50 at a time. */
    public Mono<List<MediaEntity>> fetchAllWithStatus(String status) {
        return Flux.range(1, Integer.MAX_VALUE - 1)
                .concatMap(page -> aniListRateLimiter.throttle(graphQlClient.document(BY_STATUS_QUERY)
                        .variable("page", page)
                        .variable("status", status)
                        .retrieve("Page")
                        .toEntity(Page.class)))
                .takeWhile(p -> p.getMedia() != null && !p.getMedia().isEmpty())
                .flatMapIterable(Page::getMedia)
                .map(this::convertToEntity)
                .collectList();
    }

    /** Fetches specific shows by AniList ID, in batches of 50 (one request per batch). */
    public Mono<List<MediaEntity>> fetchByIds(List<Long> ids) {
        List<List<Long>> batches = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += ID_BATCH_SIZE) {
            batches.add(ids.subList(i, Math.min(i + ID_BATCH_SIZE, ids.size())));
        }
        return Flux.fromIterable(batches)
                .concatMap(batch -> aniListRateLimiter.throttle(graphQlClient.document(BY_IDS_QUERY)
                        .variable("ids", batch)
                        .retrieve("Page")
                        .toEntity(Page.class)))
                .filter(p -> p.getMedia() != null)
                .flatMapIterable(Page::getMedia)
                .map(this::convertToEntity)
                .collectList();
    }
}
