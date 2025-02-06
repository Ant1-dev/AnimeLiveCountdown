package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.models.graphQlMedia.Media;
import com.Toine.animeCountdownBackend.models.graphQlMedia.Page;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import jakarta.transaction.Transactional;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
public class ScheduleService {
    private final MediaRepository mediaRepository;
    private final HttpGraphQlClient graphQlClient;

    public ScheduleService(HttpGraphQlClient graphQlClient, MediaRepository mediaRepository) {
        this.graphQlClient = graphQlClient;
        this.mediaRepository = mediaRepository;
    }
    @Transactional
    @Scheduled(fixedRate = 3600000)
    public void updateCurrentlyAiringAnime() {
        System.out.println("Fetching and updating currently airing anime from api..");
        saveCurrentlyAiringAnime();
    }

    @Transactional
    public void saveCurrentlyAiringAnime() {
        getCurrentlyAiringAnime()
                .flatMapMany(page -> Flux.fromIterable(page.getMedia())) // Convert Page -> List<Media>
                .map(this::convertToEntity)
                .collectList()
                .doOnNext(mediaRepository::saveAll)
                .subscribe();
    }

    public MediaEntity convertToEntity (Media media) {
        MediaEntity entity = new MediaEntity();
        entity.setId(media.getId());
        entity.setTitle_Romaji(media.getTitle().getRomaji());
        entity.setTitle_English(media.getTitle().getEnglish());
        entity.setStatus(media.getStatus());

        if (media.getNextAiringEpisode().getAiringAt() != null) {
            entity.setNext_Airing_At(Instant.ofEpochSecond(media.getNextAiringEpisode().getAiringAt()));
            entity.setNext_Airing_Episode(media.getNextAiringEpisode().getEpisode());
        } else {
            entity.setNext_Airing_At(null);
            entity.setNext_Airing_Episode(null);
        }

        entity.setCover_Image_Url(media.getCoverImage().getLarge());
        return entity;
    }


    public Mono<Page> getCurrentlyAiringAnime() {
        String query = """
                query {
                  Page(page: 1, perPage: 500) {
                    media(
                      season: WINTER
                      seasonYear: 2025
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
                .retrieve("Page")
                .toEntity(Page.class);
    }
}
