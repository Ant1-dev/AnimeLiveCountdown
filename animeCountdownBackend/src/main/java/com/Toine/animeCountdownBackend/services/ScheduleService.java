package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.models.graphQlMedia.Media;
import com.Toine.animeCountdownBackend.models.graphQlMedia.Page;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ScheduleService {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

    private final MediaRepository mediaRepository;
    private final HttpGraphQlClient graphQlClient;

    private final AtomicBoolean isUpdating = new AtomicBoolean(false);

    public ScheduleService(HttpGraphQlClient graphQlClient, MediaRepository mediaRepository) {
        this.graphQlClient = graphQlClient;
        this.mediaRepository = mediaRepository;
    }

    /**
     * Scheduled task to update the anime database
     * Runs every 5 min (300000 ms)
     */
    //@Scheduled(fixedRate = 300000) // Every 5min
    public void scheduledDatabaseRefresh() {
        // Only proceed if no update is in progress
        if (isUpdating.compareAndSet(false, true)) {
            try {
                logger.info("Starting scheduled database refresh at {}", Instant.now());

                // First fetch the data outside the transaction
                List<MediaEntity> newEntities = fetchAllCurrentlyAiringAnime().block();

                if (newEntities == null || newEntities.isEmpty()) {
                    logger.warn("No data retrieved from API, skipping database refresh");
                    return;
                }

                logger.info("Successfully fetched {} anime entries", newEntities.size());

                // Then update the database in a transaction
                updateDatabase(newEntities);

                logger.info("Database refresh completed successfully");
            } catch (Exception e) {
                logger.error("Error during scheduled refresh: {}", e.getMessage(), e);
            } finally {
                isUpdating.set(false);
            }
        } else {
            logger.info("Previous update still in progress, skipping this cycle");
        }
    }

    /**
     * Updates the database with new entities in a transaction
     * Uses an incremental approach that only updates changed records
     */
    @Transactional
    public void updateDatabase(List<MediaEntity> newEntities) {
        try {
            logger.info("Starting incremental database update...");

            // Get all existing IDs for efficient lookups and to track deletions
            List<Long> existingIds = mediaRepository.findAllIds();
            List<Long> newIds = newEntities.stream().map(MediaEntity::getId).toList();

            int updates = 0;
            int insertions = 0;
            int deletions = 0;

            // Process each new entity
            for (MediaEntity newEntity : newEntities) {
                // Check if this entity already exists
                boolean exists = existingIds.contains(newEntity.getId());

                if (exists) {
                    // Get the existing entity
                    MediaEntity existingEntity = mediaRepository.findById(newEntity.getId()).orElse(null);

                    // Check if any important fields have changed
                    if (existingEntity != null && hasSignificantChanges(existingEntity, newEntity)) {
                        // Update the entity with new values
                        updateEntityFields(existingEntity, newEntity);
                        mediaRepository.save(existingEntity);
                        updates++;
                    }
                } else {
                    // This is a new entity, so insert it
                    mediaRepository.save(newEntity);
                    insertions++;
                }
            }

            // Handle deletions (anime that are no longer airing)
            // Only if there is a complete dataset (not empty)
            if (!newIds.isEmpty()) {
                List<Long> idsToDelete = existingIds.stream()
                        .filter(id -> !newIds.contains(id))
                        .toList();

                if (!idsToDelete.isEmpty()) {
                    mediaRepository.deleteAllById(idsToDelete);
                    deletions = idsToDelete.size();
                }
            }

            logger.info("Database update completed: {} updates, {} insertions, {} deletions",
                    updates, insertions, deletions);
        } catch (Exception e) {
            logger.error("Error during incremental database update: {}", e.getMessage(), e);
            throw e; // Rethrow to trigger transaction rollback
        }
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
               !Objects.equals(existing.getSeasonYear(), newEntity.getSeasonYear());
    }

    /**
     * Update the fields of an existing entity with values from a new entity
     */
    private void updateEntityFields(MediaEntity existing, MediaEntity newEntity) {
        // Update basic info
        existing.setTitle_English(newEntity.getTitle_English());
        existing.setTitle_Romaji(newEntity.getTitle_Romaji());
        existing.setStatus(newEntity.getStatus());
        existing.setPopularity(newEntity.getPopularity());
        existing.setCover_Image_Url(newEntity.getCover_Image_Url());
        existing.setSeasonYear(newEntity.getSeasonYear());

        // Update airing info
        existing.setNext_Airing_At(newEntity.getNext_Airing_At());
        existing.setNext_Airing_Episode(newEntity.getNext_Airing_Episode());
        existing.setDay(newEntity.getDay());
    }

    /**
     * Fetches all currently airing anime
     * @return A Mono containing the list of fetched entities
     */
    public Mono<List<MediaEntity>> fetchAllCurrentlyAiringAnime() {
        logger.info("Fetching new anime data from API...");
        return fetchAllCurrentlyAiringAnimePage(1) // Start from page 1
                .map(this::convertToEntity)
                .collectList();
    }

    public MediaEntity convertToEntity(Media media) {
        MediaEntity entity = new MediaEntity();
        entity.setId(media.getId());
        entity.setTitle_Romaji(media.getTitle().getRomaji());
        entity.setTitle_English(media.getTitle().getEnglish());
        entity.setStatus(media.getStatus());
        entity.setPopularity(media.getPopularity());
        entity.setSeasonYear(media.getSeasonYear());

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

        entity.setCover_Image_Url(media.getCoverImage().getLarge());
        return entity;
    }

    public Flux<Media> fetchAllCurrentlyAiringAnimePage(int page) {
        String query = """
        query ($page: Int) {
          Page(page: $page, perPage: 50) {
            media(
              isAdult: false
              type: ANIME
              status_in: [RELEASING, NOT_YET_RELEASED]
              sort: POPULARITY_DESC
            ) {
              id
              popularity
              title {
                english
                romaji
              }
              status
              seasonYear
              nextAiringEpisode {
                airingAt
                episode
              }
              coverImage {
                large
              }
            }
          }
        }
    """;

        return graphQlClient.document(query)
                .variable("page", page)
                .retrieve("Page")
                .toEntity(Page.class)
                .flatMapMany(p -> {
                    if (p.getMedia().isEmpty()) {
                        return Flux.empty(); // Stop when no more data
                    }
                    return Flux.fromIterable(p.getMedia())
                            .concatWith(fetchAllCurrentlyAiringAnimePage(page + 1)); // Recursively fetch next page
                });
    }
}