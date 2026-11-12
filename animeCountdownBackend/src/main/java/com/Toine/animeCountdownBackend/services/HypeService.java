package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.dto.HypeEntry;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.repositories.EpisodeReactionRepository;
import com.Toine.animeCountdownBackend.repositories.FollowerCountRepository;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** "Most hyped this week": currently airing shows ranked by recent reactions and followers. */
@Service
public class HypeService {

    static final Duration WINDOW = Duration.ofDays(7);
    static final Duration CACHE_TTL = Duration.ofMinutes(5);
    public static final int MAX_LIMIT = 30;

    private final EpisodeReactionRepository reactionRepository;
    private final FollowerCountRepository followerCountRepository;
    private final MediaRepository mediaRepository;
    private final Clock clock;

    private volatile Cached cached;

    @Autowired
    public HypeService(EpisodeReactionRepository reactionRepository, FollowerCountRepository followerCountRepository,
                       MediaRepository mediaRepository) {
        this(reactionRepository, followerCountRepository, mediaRepository, Clock.systemUTC());
    }

    HypeService(EpisodeReactionRepository reactionRepository, FollowerCountRepository followerCountRepository,
                MediaRepository mediaRepository, Clock clock) {
        this.reactionRepository = reactionRepository;
        this.followerCountRepository = followerCountRepository;
        this.mediaRepository = mediaRepository;
        this.clock = clock;
    }

    /** Top shows, cached for a few minutes. */
    public List<HypeEntry> mostHyped(int limit) {
        int capped = Math.min(Math.max(limit, 1), MAX_LIMIT);
        Instant now = clock.instant();
        Cached current = cached;
        if (current == null || now.isAfter(current.computedAt().plus(CACHE_TTL))) {
            current = new Cached(now, rank(now, MAX_LIMIT));
            cached = current;
        }
        return current.entries().stream().limit(capped).toList();
    }

    /** Computes the ranking without the cache. */
    @Transactional(readOnly = true)
    public List<HypeEntry> rank(Instant now, int limit) {
        Instant since = now.minus(WINDOW);
        Map<Long, Long> reactions = toMap(reactionRepository.countRecentByMedia(since));
        Map<Long, Long> newFollowers = toMap(followerCountRepository.countNewFollowersByMedia(since));
        Map<Long, Long> followers = toMap(followerCountRepository.countFollowersByMedia());

        Set<Long> candidateIds = new HashSet<>(reactions.keySet());
        candidateIds.addAll(newFollowers.keySet());
        candidateIds.addAll(followers.keySet());

        Map<Long, MediaEntity> airing = mediaRepository.findAllById(candidateIds).stream()
                .filter(m -> "RELEASING".equals(m.getStatus()))
                .collect(Collectors.toMap(MediaEntity::getId, Function.identity()));
        Map<Long, Long> popularity = new HashMap<>();
        airing.values().forEach(m -> popularity.put(m.getId(), Objects.requireNonNullElse(m.getPopularity(), 0L)));

        return HypeRanker.rank(reactions, newFollowers, followers, airing::containsKey, popularity, limit).stream()
                .map(r -> new HypeEntry(airing.get(r.mediaId()), r.score(), r.recentReactions(),
                        r.newFollowers(), r.followers()))
                .toList();
    }

    private static Map<Long, Long> toMap(List<EpisodeReactionRepository.MediaCount> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (EpisodeReactionRepository.MediaCount row : rows) {
            map.put(row.getMediaId(), row.getTotal());
        }
        return map;
    }

    private record Cached(Instant computedAt, List<HypeEntry> entries) {
    }
}
