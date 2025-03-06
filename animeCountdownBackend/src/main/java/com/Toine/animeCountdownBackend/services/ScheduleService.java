package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.models.graphQlMedia.Media;
import com.Toine.animeCountdownBackend.models.graphQlMedia.Page;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ScheduleService {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

    private final MediaRepository mediaRepository;
    private final HttpGraphQlClient graphQlClient;
    private final ObjectMapper objectMapper;

    private final AtomicBoolean isUpdating = new AtomicBoolean(false);

    public ScheduleService(HttpGraphQlClient graphQlClient, MediaRepository mediaRepository, ObjectMapper objectMapper) {
        this.graphQlClient = graphQlClient;
        this.mediaRepository = mediaRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Scheduled task to update the anime database
     * Runs every 30 min (1800000 ms)
     */
    @Scheduled(fixedRate = 1800000) // Every 30min
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

                logger.info("Database refresh completed successfully with {} entries", newEntities.size());
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
     */
    @Transactional
    public void updateDatabase(List<MediaEntity> newEntities) {
        try {
            logger.info("Clearing existing anime data from database...");
            mediaRepository.deleteAll();

            logger.info("Saving fresh anime data to database...");
            mediaRepository.saveAll(newEntities);
        } catch (Exception e) {
            logger.error("Error during database update: {}", e.getMessage(), e);
            throw e; // Rethrow to trigger transaction rollback
        }
    }

    /**
     * Fetches all currently airing anime
     * @return A Mono containing the list of fetched entities
     */
    public Mono<List<MediaEntity>> fetchAllCurrentlyAiringAnime() {
        logger.info("Fetching fresh anime data from API...");
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
              status: RELEASING
              sort: POPULARITY_DESC
            ) {
              id
              title {
                english
                romaji
              }
              status
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