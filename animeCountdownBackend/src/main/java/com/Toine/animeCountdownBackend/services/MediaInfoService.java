package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.models.graphQlMedia.MediaInfo;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import com.Toine.animeCountdownBackend.repositories.MediaInfoRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class MediaInfoService {
    private static final Logger logger = LoggerFactory.getLogger(MediaInfoService.class);
    private final HttpGraphQlClient graphQlClient;
    private final MediaInfoRepository mediaInfoRepository;
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);

    public MediaInfoService(HttpGraphQlClient graphQlClient, MediaInfoRepository mediaInfoRepository) {
        this.graphQlClient = graphQlClient;
        this.mediaInfoRepository = mediaInfoRepository;
    }

    @Scheduled(fixedRate = 43200000) //every 3hr
    public void scheduledDatabaseRefresh() {
        // Only proceed if no update is in progress
        if (isUpdating.compareAndSet(false, true)) {
            try {
                logger.info("Starting scheduled media_info refresh at {}", Instant.now());

                // First fetch the data outside the transaction
                List<MediaInfoEntity> newEntities = fetchAllCurrentlyAiringAnimeInfo().block();

                if (newEntities == null || newEntities.isEmpty()) {
                    logger.warn("No data retrieved from API, skipping database refresh");
                    return;
                }

                logger.info("Successfully fetched {} anime info entries", newEntities.size());

                // Then update the database in a transaction
                updateDatabase(newEntities);

                logger.info("Media info database refresh completed successfully");
            } catch (Exception e) {
                logger.error("There was an error trying to refresh data from graphql api!", e);
            } finally {
                isUpdating.set(false);
            }
        } else {
            logger.info("Previous media info update still in progress, skipping this cycle");
        }
    }

    /**
     * Updates the database with new entities in a transaction
     * Uses an incremental approach that only updates changed records
     * Optimized with batch operations and HashSet lookups
     */
    @Transactional
    public void updateDatabase(List<MediaInfoEntity> newEntities) {
        try {
            logger.info("Starting incremental media info database update...");

            // Get all existing IDs - use HashSet for O(1) lookup
            Set<Long> existingIds = new HashSet<>(mediaInfoRepository.findAllIds());
            Set<Long> newIds = newEntities.stream()
                    .map(MediaInfoEntity::getId)
                    .collect(Collectors.toSet());

            // Fetch all existing entities at once to avoid N+1 queries
            Map<Long, MediaInfoEntity> existingEntitiesMap = mediaInfoRepository
                    .findAllById(existingIds)
                    .stream()
                    .collect(Collectors.toMap(MediaInfoEntity::getId, entity -> entity));

            List<MediaInfoEntity> entitiesToUpdate = new ArrayList<>();
            List<MediaInfoEntity> entitiesToInsert = new ArrayList<>();

            int updates = 0;
            int insertions = 0;
            int deletions = 0;

            // Process each new entity
            for (MediaInfoEntity newEntity : newEntities) {
                MediaInfoEntity existingEntity = existingEntitiesMap.get(newEntity.getId());

                if (existingEntity != null) {
                    // Check if any important fields have changed
                    if (hasSignificantChanges(existingEntity, newEntity)) {
                        // Update the entity with new values
                        updateEntityFields(existingEntity, newEntity);
                        entitiesToUpdate.add(existingEntity);
                        updates++;
                    }
                } else {
                    // This is a new entity, so insert it
                    entitiesToInsert.add(newEntity);
                    insertions++;
                }
            }

            // Batch save all updates and insertions
            if (!entitiesToUpdate.isEmpty()) {
                mediaInfoRepository.saveAll(entitiesToUpdate);
            }
            if (!entitiesToInsert.isEmpty()) {
                mediaInfoRepository.saveAll(entitiesToInsert);
            }

            // Handle deletions (anime that are no longer airing)
            // Only if there is a complete dataset (not empty)
            if (!newIds.isEmpty()) {
                Set<Long> idsToDelete = existingIds.stream()
                        .filter(id -> !newIds.contains(id))
                        .collect(Collectors.toSet());

                if (!idsToDelete.isEmpty()) {
                    mediaInfoRepository.deleteAllById(idsToDelete);
                    deletions = idsToDelete.size();
                }
            }

            logger.info("Media info database update completed: {} updates, {} insertions, {} deletions",
                    updates, insertions, deletions);
        } catch (Exception e) {
            logger.error("Error during incremental media info database update: {}", e.getMessage(), e);
            throw e; // Rethrow to trigger transaction rollback
        }
    }

    /**
     * Check if a media info entity has significant changes that require an update
     */
    private boolean hasSignificantChanges(MediaInfoEntity existing, MediaInfoEntity newEntity) {
        if (existing == null || newEntity == null) {
            return true; // Null check to prevent NullPointerException
        }

        return !Objects.equals(existing.getEngtitle(), newEntity.getEngtitle()) ||
               !Objects.equals(existing.getRomtitle(), newEntity.getRomtitle()) ||
               !Objects.equals(existing.getStatus(), newEntity.getStatus()) ||
               !Objects.equals(existing.getPopularity(), newEntity.getPopularity()) ||
               !Objects.equals(existing.getSeason(), newEntity.getSeason()) ||
               !Objects.equals(existing.getSeasonYear(), newEntity.getSeasonYear()) ||
               !Objects.equals(existing.getNextepisode(), newEntity.getNextepisode()) ||
               !Objects.equals(existing.getAiringat(), newEntity.getAiringat()) ||
               !Objects.equals(existing.getTotalepisodes(), newEntity.getTotalepisodes()) ||
               !Objects.equals(existing.getDuration(), newEntity.getDuration()) ||
               !Objects.equals(existing.getGenres(), newEntity.getGenres()) ||
               !Objects.equals(existing.getAvgscore(), newEntity.getAvgscore()) ||
               !Objects.equals(existing.getDescription(), newEntity.getDescription()) ||
               !Objects.equals(existing.getCoverimage(), newEntity.getCoverimage()) ||
               !Objects.equals(existing.getBanner(), newEntity.getBanner());
    }

    /**
     * Update the fields of an existing entity with values from a new entity
     */
    private void updateEntityFields(MediaInfoEntity existing, MediaInfoEntity newEntity) {
        // Update basic info
        existing.setEngtitle(newEntity.getEngtitle());
        existing.setRomtitle(newEntity.getRomtitle());
        existing.setStatus(newEntity.getStatus());
        existing.setPopularity(newEntity.getPopularity());
        existing.setSeason(newEntity.getSeason());
        existing.setSeasonYear(newEntity.getSeasonYear());

        // Update episode info
        existing.setNextepisode(newEntity.getNextepisode());
        existing.setAiringat(newEntity.getAiringat());
        existing.setTotalepisodes(newEntity.getTotalepisodes());
        existing.setDuration(newEntity.getDuration());

        // Update media details
        existing.setGenres(newEntity.getGenres());
        existing.setAvgscore(newEntity.getAvgscore());
        existing.setDescription(newEntity.getDescription());
        existing.setCoverimage(newEntity.getCoverimage());
        existing.setBanner(newEntity.getBanner());
    }

    public MediaInfoEntity convertToEntity(MediaInfo mediaInfo) {
        MediaInfoEntity entity = new MediaInfoEntity();

        entity.setId(mediaInfo.getId());

        entity.setEngtitle(mediaInfo.getTitle().getEnglish());
        entity.setRomtitle(mediaInfo.getTitle().getRomaji());
        entity.setStatus(mediaInfo.getStatus());
        entity.setPopularity(mediaInfo.getPopularity());
        entity.setSeason(mediaInfo.getSeason());
        entity.setSeasonYear(mediaInfo.getSeasonYear());

        if (mediaInfo.getNextAiringEpisode() != null && mediaInfo.getNextAiringEpisode().getAiringAt() != null) {
            entity.setNextepisode(mediaInfo.getNextAiringEpisode().getEpisode());
            Instant airingTime = Instant.ofEpochSecond(mediaInfo.getNextAiringEpisode().getAiringAt());
            entity.setAiringat(airingTime);
        }
        entity.setTotalepisodes(mediaInfo.getEpisodes());
        entity.setDuration(mediaInfo.getDuration());

        entity.setGenres(mediaInfo.getGenres());
        entity.setAvgscore(mediaInfo.getAverageScore());
        entity.setDescription(mediaInfo.getDescription());

        entity.setCoverimage(mediaInfo.getCoverImage().getExtraLarge());
        entity.setBanner(mediaInfo.getBannerImage());

        return entity;
    }

    // maps all the media gotten from the graphql call to lists from the first page
    public Mono<List<MediaInfoEntity>> fetchAllCurrentlyAiringAnimeInfo() {
        logger.info("Getting information of releasing anime...");
        return fetchReleasingAnimeInfoPage(1)
                .map(this::convertToEntity)
                .collectList();
    }

    // graphql call gets all the pages for media info
    // Uses iterative approach with expand for better performance than recursive concatWith
    public Flux<MediaInfo> fetchReleasingAnimeInfoPage(int page) {
        String query =
                """
                query ($page: Int) {
                  Page(page: $page, perPage: 50) {
                    media(
                      isAdult: false
                      type: ANIME
                      status_in: [RELEASING, NOT_YET_RELEASED]
                      sort: POPULARITY_DESC
                    ) {
                      id
                      title {
                        english
                        romaji
                      }
                      nextAiringEpisode {
                        episode
                        airingAt
                      }
                      seasonYear
                      season
                      bannerImage
                      coverImage {
                        extraLarge
                      }
                      status
                      popularity
                      episodes
                      duration
                      genres
                      averageScore
                      description
                    }
                  }
                }
                """;

        return Flux.range(page, Integer.MAX_VALUE - page)
                .concatMap(currentPage ->
                    graphQlClient.document(query)
                        .variable("page", currentPage)
                        .retrieve("Page.media")
                        .toEntityList(MediaInfo.class)
                )
                .takeWhile(mediaList -> !mediaList.isEmpty())
                .flatMapIterable(mediaList -> mediaList);
    }
}