package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.models.graphQlMedia.MediaInfo;
import com.Toine.animeCountdownBackend.models.graphQlMedia.Page;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class MediaInfoService {
    private static final Logger logger = LoggerFactory.getLogger(MediaInfoService.class);
    private final HttpGraphQlClient graphQlClient;

    public MediaInfoService(HttpGraphQlClient graphQlClient) {
        this.graphQlClient = graphQlClient;
    }

    public MediaInfoEntity convertToEntity(MediaInfo mediaInfo) {
        MediaInfoEntity entity = new MediaInfoEntity();

        entity.setAvgscore(mediaInfo.getAverageScore());
        entity.setBanner(mediaInfo.getBannerImage());
        entity.setDescription(mediaInfo.getDescription());
        entity.setDuration(mediaInfo.getDuration());
        entity.setGenres(mediaInfo.getGenres());
        entity.setCoverimage(mediaInfo.getCoverImage().getLarge());
        entity.setEngtitle(mediaInfo.getTitle().getEnglish());
        entity.setRomtitle(mediaInfo.getTitle().getRomaji());
        entity.setNextepisode(mediaInfo.getNextAiringEpisode().getEpisode());
        entity.setTotalepisodes(mediaInfo.getEpisodes());
        entity.setPopularity(mediaInfo.getPopularity());
        entity.setStatus(mediaInfo.getStatus());

        return entity;
    }

    public Mono<List<MediaInfoEntity>> fetchAllCurrentlyAiringAnimeInfo() {
        logger.info("Getting information of releasing anime...");
        return fetchReleasingAnimeInfoPage(1)
                .map(this::convertToEntity)
                .collectList();
    }


    public Flux<MediaInfo> fetchReleasingAnimeInfoPage(int page) {
        String query =
                """
                query {
                  Page(page: 1, perPage: 50) { \s
                    media(
                      isAdult: false
                      type: ANIME
                      status: RELEASING
                      sort: POPULARITY_DESC
                    ) {
                      title {
                        english
                        romaji
                      }
                      nextAiringEpisode {
                        episode
                        airingAt
                      }
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
                .retrieve("Page")
                .toEntity(Page.class)
                .flatMapMany( p -> {
                    if (p.getMediaInfo().isEmpty()) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(p.getMediaInfo())
                            .concatWith(fetchReleasingAnimeInfoPage(page + 1));
                });
    }

}
