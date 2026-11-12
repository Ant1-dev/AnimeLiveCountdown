package com.Toine.animeCountdownBackend.services.watchorder;

import com.Toine.animeCountdownBackend.components.AniListRateLimiter;
import com.Toine.animeCountdownBackend.repositories.MediaInfoRepository;
import com.Toine.animeCountdownBackend.services.watchorder.RelationGraph.Node;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Watch order for a show, built from AniList relations on demand and cached for a day.
 *
 * One request fetches relations three levels deep; long franchises continue from the unexplored edge
 * with up to three more requests. All requests go through the shared AniListRateLimiter.
 */
@Service
public class WatchOrderService {
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final int MAX_EXTRA_REQUESTS = 3;
    private static final int MAX_CACHED = 2000;
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private static final String NODE_FIELDS = """
            id type format status episodes
            startDate { year month day }
            title { english romaji }
            coverImage { medium }
            """;

    static final String QUERY = """
            query ($id: Int) {
              Media(id: $id, type: ANIME) {
                %1$s
                relations { edges { relationType node {
                  %1$s
                  relations { edges { relationType node {
                    %1$s
                    relations { edges { relationType node { %1$s } } }
                  } } }
                } } }
              }
            }
            """.formatted(NODE_FIELDS);

    private record Cached(List<WatchOrderEntry> entries, Instant expiresAt) {
    }

    private final HttpGraphQlClient graphQlClient;
    private final AniListRateLimiter rateLimiter;
    private final MediaInfoRepository mediaInfoRepository;
    private final Map<Long, Cached> cache = new ConcurrentHashMap<>();

    public WatchOrderService(HttpGraphQlClient graphQlClient, AniListRateLimiter rateLimiter,
                             MediaInfoRepository mediaInfoRepository) {
        this.graphQlClient = graphQlClient;
        this.rateLimiter = rateLimiter;
        this.mediaInfoRepository = mediaInfoRepository;
    }

    public List<WatchOrderEntry> watchOrder(long mediaId) {
        Cached cached = cache.get(mediaId);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.entries();
        }

        List<Node> graphs = new ArrayList<>();
        graphs.add(fetch(mediaId));
        Map<Long, Node> byId = WatchOrderBuilder.index(graphs);
        for (int i = 0; i < MAX_EXTRA_REQUESTS; i++) {
            List<Long> frontier = WatchOrderBuilder.frontier(byId, mediaId);
            if (frontier.isEmpty()) {
                break;
            }
            graphs.add(fetch(frontier.get(0)));
            byId = WatchOrderBuilder.index(graphs);
        }

        Set<Long> onSite = new HashSet<>();
        mediaInfoRepository.findAllById(byId.keySet()).forEach(m -> onSite.add(m.getId()));
        List<WatchOrderEntry> entries = WatchOrderBuilder.build(byId, mediaId, onSite);

        if (cache.size() >= MAX_CACHED) {
            cache.clear();
        }
        // Every member of the franchise has the same list, apart from which entry is "current"
        cache.put(mediaId, new Cached(entries, Instant.now().plus(CACHE_TTL)));
        return entries;
    }

    private Node fetch(long id) {
        Node node = rateLimiter.throttle(graphQlClient.document(QUERY)
                        .variable("id", id)
                        .retrieve("Media")
                        .toEntity(Node.class))
                .block(TIMEOUT);
        if (node == null) {
            throw new IllegalStateException("AniList returned no media for " + id);
        }
        return node;
    }
}
