package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.dto.ReactionSummary;
import com.Toine.animeCountdownBackend.models.ReactionType;
import com.Toine.animeCountdownBackend.models.postgreEntities.EpisodeReactionEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.EpisodeReactionRepository;
import com.Toine.animeCountdownBackend.repositories.MediaInfoRepository;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Episode reactions. Rules:
 * <ul>
 *   <li>one reaction per user per episode; reacting again replaces it</li>
 *   <li>only episodes that have aired can be reacted to</li>
 *   <li>counts are public</li>
 * </ul>
 */
@Service
public class ReactionService {

    private static final Map<ReactionType, String> EMOJIS = new EnumMap<>(ReactionType.class);

    static {
        for (ReactionType type : ReactionType.values()) {
            EMOJIS.put(type, type.getEmoji());
        }
    }

    private final EpisodeReactionRepository reactionRepository;
    private final MediaRepository mediaRepository;
    private final MediaInfoRepository mediaInfoRepository;
    private final Clock clock;

    @Autowired
    public ReactionService(EpisodeReactionRepository reactionRepository, MediaRepository mediaRepository,
                           MediaInfoRepository mediaInfoRepository) {
        this(reactionRepository, mediaRepository, mediaInfoRepository, Clock.systemUTC());
    }

    ReactionService(EpisodeReactionRepository reactionRepository, MediaRepository mediaRepository,
                    MediaInfoRepository mediaInfoRepository, Clock clock) {
        this.reactionRepository = reactionRepository;
        this.mediaRepository = mediaRepository;
        this.mediaInfoRepository = mediaInfoRepository;
        this.clock = clock;
    }

    /** Counts for the latest aired episode of a show (episode is null if none has aired). */
    @Transactional(readOnly = true)
    public ReactionSummary latestSummary(Long mediaId, UserEntity viewer) {
        Optional<Integer> latest = latestAiredEpisode(mediaId);
        return latest.map(episode -> buildSummary(mediaId, episode, viewer))
                .orElseGet(() -> new ReactionSummary(mediaId, null, zeroCounts(), 0, null, EMOJIS));
    }

    /** Counts for a specific aired episode. */
    @Transactional(readOnly = true)
    public ReactionSummary summary(Long mediaId, int episode, UserEntity viewer) {
        requireAired(mediaId, episode);
        return buildSummary(mediaId, episode, viewer);
    }

    /** Sets (or replaces) the user's reaction to an episode. Reacting with the same emoji again changes nothing. */
    @Transactional
    public ReactionSummary react(UserEntity user, Long mediaId, int episode, String reactionName) {
        requireUser(user);
        ReactionType reaction = ReactionType.parse(reactionName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown reaction"));
        requireAired(mediaId, episode);

        Instant now = clock.instant();
        Optional<EpisodeReactionEntity> existing = reactionRepository.findByUserAndEpisode(user.getId(), mediaId, episode);
        if (existing.isPresent()) {
            EpisodeReactionEntity entity = existing.get();
            if (entity.getReaction() != reaction) {
                entity.setReaction(reaction);
                entity.setUpdatedAt(now);
                reactionRepository.save(entity);
            }
        } else {
            EpisodeReactionEntity entity = new EpisodeReactionEntity();
            entity.setUser(user);
            entity.setMediaId(mediaId);
            entity.setEpisode(episode);
            entity.setReaction(reaction);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            // saveAndFlush so a concurrent duplicate fails here (unique constraint) rather than at commit
            reactionRepository.saveAndFlush(entity);
        }
        return buildSummary(mediaId, episode, user);
    }

    /** Removes the user's reaction to an episode, if any. */
    @Transactional
    public ReactionSummary remove(UserEntity user, Long mediaId, int episode) {
        requireUser(user);
        requireShow(mediaId);
        reactionRepository.deleteByUserAndEpisode(user.getId(), mediaId, episode);
        return buildSummary(mediaId, episode, user);
    }

    /** The latest episode of a show that has aired, or empty if none has. 404 if the show is unknown. */
    public Optional<Integer> latestAiredEpisode(Long mediaId) {
        ShowState show = requireShow(mediaId);
        return LatestEpisode.latestAired(show.status(), show.nextEpisode(), show.nextAiringAt(),
                show.totalEpisodes(), clock.instant());
    }

    private void requireAired(Long mediaId, int episode) {
        int latest = latestAiredEpisode(mediaId).orElse(0);
        if (episode < 1 || episode > latest) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That episode hasn't aired yet");
        }
    }

    private static void requireUser(UserEntity user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to react");
        }
    }

    private ReactionSummary buildSummary(Long mediaId, int episode, UserEntity viewer) {
        Map<ReactionType, Long> counts = zeroCounts();
        long total = 0;
        for (EpisodeReactionRepository.ReactionCount row : reactionRepository.countByEpisode(mediaId, episode)) {
            counts.put(row.getReaction(), row.getTotal());
            total += row.getTotal();
        }
        ReactionType mine = viewer == null ? null
                : reactionRepository.findByUserAndEpisode(viewer.getId(), mediaId, episode)
                        .map(EpisodeReactionEntity::getReaction)
                        .orElse(null);
        return new ReactionSummary(mediaId, episode, counts, total, mine, EMOJIS);
    }

    private static Map<ReactionType, Long> zeroCounts() {
        Map<ReactionType, Long> counts = new EnumMap<>(ReactionType.class);
        for (ReactionType type : ReactionType.values()) {
            counts.put(type, 0L);
        }
        return counts;
    }

    /**
     * Schedule state of a show. The media table (synced every minute) wins for status and next
     * episode; media_info fills in the episode count and covers shows that left the schedule.
     */
    private ShowState requireShow(Long mediaId) {
        Optional<MediaEntity> media = mediaRepository.findById(mediaId);
        Optional<MediaInfoEntity> info = mediaInfoRepository.findById(mediaId);
        if (media.isEmpty() && info.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Show not found");
        }
        String status = media.map(MediaEntity::getStatus).orElseGet(() -> info.get().getStatus());
        Integer next = media.map(MediaEntity::getNext_Airing_Episode)
                .orElseGet(() -> info.map(MediaInfoEntity::getNextepisode).orElse(null));
        Instant nextAt = media.map(MediaEntity::getNext_Airing_At)
                .orElseGet(() -> info.map(MediaInfoEntity::getAiringat).orElse(null));
        Integer total = info.map(MediaInfoEntity::getTotalepisodes).orElse(null);
        return new ShowState(status, next, nextAt, total);
    }

    private record ShowState(String status, Integer nextEpisode, Instant nextAiringAt, Integer totalEpisodes) {
    }
}
