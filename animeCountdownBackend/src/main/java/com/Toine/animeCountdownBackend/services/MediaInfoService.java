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
import java.util.List;


@Service
public class MediaInfoService {
    private static final Logger logger = LoggerFactory.getLogger(MediaInfoService.class);
    private final HttpGraphQlClient graphQlClient;
    private final MediaInfoRepository mediaInfoRepository;

    public MediaInfoService(HttpGraphQlClient graphQlClient, MediaInfoRepository mediaInfoRepository) {
        this.graphQlClient = graphQlClient;
        this.mediaInfoRepository = mediaInfoRepository;
    }

    @Scheduled(fixedRate = 43200000) //every 3hr
    public void scheduledDatabaseRefresh() {
        // Only proceed if no update is in progress
        logger.info("Starting scheduled media_info refresh at {}", Instant.now());

        // First fetch the data outside the transaction
        List<MediaInfoEntity> newEntities = fetchAllCurrentlyAiringAnimeInfo().block();

        logger.info("Clearing Database");
        mediaInfoRepository.deleteAll();
        if (newEntities != null) {
            logger.info("Successfully fetched {} anime info entries", newEntities.size());
            mediaInfoRepository.saveAll(newEntities);
            logger.info("Saved {} anime info entries", newEntities.size());
        } else {
            logger.info("Nothing to save");
        }
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

        entity.setCoverimage(mediaInfo.getCoverImage().getLarge());
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
                        large
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
        return graphQlClient.document(query)
                .variable("page", page)
                .retrieve("Page.media")
                .toEntityList(MediaInfo.class)
                .flatMapMany(mediaList -> {
                    if (mediaList.isEmpty()) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(mediaList)
                            .concatWith(fetchReleasingAnimeInfoPage(page + 1));
                });
    }

}
