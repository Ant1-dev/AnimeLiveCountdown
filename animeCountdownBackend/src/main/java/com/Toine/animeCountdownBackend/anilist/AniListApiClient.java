package com.Toine.animeCountdownBackend.anilist;

import com.Toine.animeCountdownBackend.anilist.AniListModels.ListCollection;
import com.Toine.animeCountdownBackend.anilist.AniListModels.ListEntry;
import com.Toine.animeCountdownBackend.anilist.AniListModels.ListGroup;
import com.Toine.animeCountdownBackend.anilist.AniListModels.TokenResponse;
import com.Toine.animeCountdownBackend.anilist.AniListModels.Viewer;
import com.Toine.animeCountdownBackend.components.AniListRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Calls AniList on behalf of a signed-in user (with their AniList token). Every request, the token
 * exchange included, goes through the shared AniListRateLimiter.
 */
@Component
public class AniListApiClient {
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    static final String VIEWER_QUERY = """
            query {
              Viewer {
                id
                name
              }
            }
            """;

    static final String LIST_QUERY = """
            query ($userId: Int) {
              MediaListCollection(userId: $userId, type: ANIME, status_in: [CURRENT, PLANNING]) {
                lists {
                  name
                  entries {
                    status
                    progress
                    media {
                      id
                      isAdult
                      status
                      episodes
                      popularity
                      seasonYear
                      season
                      bannerImage
                      title {
                        english
                        romaji
                      }
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
              }
            }
            """;

    private final AniListRateLimiter rateLimiter;
    private final AniListOAuthSettings settings;
    private final WebClient graphQlWebClient;
    private final WebClient tokenWebClient;
    private final String tokenUrl;

    public AniListApiClient(AniListRateLimiter rateLimiter, AniListOAuthSettings settings,
                            @Value("${app.anilist.graphql-url:https://graphql.anilist.co}") String graphQlUrl,
                            @Value("${app.anilist.token-url:" + AniListOAuthSettings.TOKEN_URL + "}") String tokenUrl) {
        this.rateLimiter = rateLimiter;
        this.settings = settings;
        this.tokenUrl = tokenUrl;
        this.graphQlWebClient = WebClient.builder()
                .baseUrl(graphQlUrl)
                // Large PLANNING lists can exceed WebClient's 256 KB default
                .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
                .build();
        this.tokenWebClient = WebClient.builder().build();
    }

    /** Exchanges an authorization code for an access token (AniList tokens last a year; no refresh token). */
    public TokenResponse exchangeCode(String code, String redirectUri) {
        Map<String, String> body = Map.of(
                "grant_type", "authorization_code",
                "client_id", settings.clientId(),
                "client_secret", settings.clientSecret(),
                "redirect_uri", redirectUri,
                "code", code);
        TokenResponse token = rateLimiter.throttle(tokenWebClient.post()
                        .uri(tokenUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(TokenResponse.class))
                .block(TIMEOUT);
        if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
            throw new IllegalStateException("AniList returned no access token");
        }
        return token;
    }

    public Viewer fetchViewer(String accessToken) {
        Viewer viewer = rateLimiter.throttle(client(accessToken).document(VIEWER_QUERY)
                        .retrieve("Viewer")
                        .toEntity(Viewer.class))
                .block(TIMEOUT);
        if (viewer == null || viewer.id() == null) {
            throw new IllegalStateException("AniList returned no viewer");
        }
        return viewer;
    }

    /** The user's CURRENT and PLANNING anime entries, in one request. */
    public List<ListEntry> fetchCurrentAndPlanning(String accessToken, long aniListUserId) {
        ListCollection collection = rateLimiter.throttle(client(accessToken).document(LIST_QUERY)
                        .variable("userId", aniListUserId)
                        .retrieve("MediaListCollection")
                        .toEntity(ListCollection.class))
                .block(TIMEOUT);
        return flatten(collection);
    }

    static List<ListEntry> flatten(ListCollection collection) {
        List<ListEntry> entries = new ArrayList<>();
        if (collection == null || collection.lists() == null) {
            return entries;
        }
        for (ListGroup group : collection.lists()) {
            if (group != null && group.entries() != null) {
                entries.addAll(group.entries());
            }
        }
        return entries;
    }

    private HttpGraphQlClient client(String accessToken) {
        return HttpGraphQlClient.builder(graphQlWebClient)
                .header("Authorization", "Bearer " + accessToken)
                .build();
    }
}
