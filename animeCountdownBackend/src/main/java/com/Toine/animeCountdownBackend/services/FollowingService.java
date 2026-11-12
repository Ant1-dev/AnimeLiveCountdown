package com.Toine.animeCountdownBackend.services;

import com.Toine.animeCountdownBackend.anilist.AniListSyncService;
import com.Toine.animeCountdownBackend.dto.FollowView;
import com.Toine.animeCountdownBackend.models.postgreEntities.FavoriteMediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaInfoEntity;
import com.Toine.animeCountdownBackend.repositories.FavoriteMediaRepository;
import com.Toine.animeCountdownBackend.repositories.MediaInfoRepository;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Follows ("Following", stored in favmedia) and episode progress. Every method takes the signed-in
 * user's ID and only ever reads or changes that user's rows.
 */
@Service
public class FollowingService {
    private static final Set<String> SCHEDULED_STATUSES = Set.of("RELEASING", "NOT_YET_RELEASED");
    // Upper bound for progress on shows whose aired count is unknown
    static final int MAX_EPISODE = 5000;

    private final FavoriteMediaRepository favoriteRepository;
    private final MediaRepository mediaRepository;
    private final MediaInfoRepository mediaInfoRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /** Pushes follows and progress to the user's AniList list; absent in tests. */
    private AniListSyncService aniListSync;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setAniListSync(AniListSyncService aniListSync) {
        this.aniListSync = aniListSync;
    }

    @Value("${app.media.buffer-days:14}")
    private int bufferDays = 14;

    @org.springframework.beans.factory.annotation.Autowired
    public FollowingService(FavoriteMediaRepository favoriteRepository, MediaRepository mediaRepository,
                            MediaInfoRepository mediaInfoRepository, UserRepository userRepository) {
        this(favoriteRepository, mediaRepository, mediaInfoRepository, userRepository, Clock.systemUTC());
    }

    FollowingService(FavoriteMediaRepository favoriteRepository, MediaRepository mediaRepository,
                     MediaInfoRepository mediaInfoRepository, UserRepository userRepository, Clock clock) {
        this.favoriteRepository = favoriteRepository;
        this.mediaRepository = mediaRepository;
        this.mediaInfoRepository = mediaInfoRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<FollowView> list(long userId) {
        List<FavoriteMediaEntity> follows = favoriteRepository.findFollowsWithMedia(userId);
        Map<Long, Integer> totals = totalEpisodes(follows);
        Instant now = clock.instant();
        return follows.stream()
                .map(f -> view(f, totals.get(f.getMedia().getId()), now))
                .toList();
    }

    @Transactional
    public FollowView follow(long userId, long mediaId) {
        Optional<FavoriteMediaEntity> existing = favoriteRepository.findFollow(userId, mediaId);
        if (existing.isPresent()) {
            return view(existing.get());
        }
        MediaEntity media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Show not found"));
        FavoriteMediaEntity follow = new FavoriteMediaEntity();
        follow.setUser(userRepository.getReferenceById(userId));
        follow.setMedia(media);
        follow.setAddedDate(clock.instant());
        follow.setWatchedEpisode(0);
        try {
            favoriteRepository.saveAndFlush(follow);
        } catch (DataIntegrityViolationException e) {
            // Followed concurrently (double click); the row exists now
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already following");
        }
        if (aniListSync != null) {
            aniListSync.followed(userId, mediaId);
        }
        return view(follow);
    }

    @Transactional
    public void unfollow(long userId, long mediaId) {
        if (favoriteRepository.removeFavoriteMedia(userId, mediaId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "You don't follow this show");
        }
    }

    /** Sets the highest episode watched. 404 if this user doesn't follow the show. */
    @Transactional
    public FollowView setProgress(long userId, long mediaId, int watchedEpisode) {
        FavoriteMediaEntity follow = favoriteRepository.findFollow(userId, mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You don't follow this show"));
        Instant now = clock.instant();
        Integer total = totalEpisodes(List.of(follow)).get(mediaId);
        Integer latest = EpisodeProgress.latestAired(follow.getMedia(), total, now);
        int max = latest != null ? latest : MAX_EPISODE;
        if (watchedEpisode < 0 || watchedEpisode > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    latest != null ? "Only episodes 0-" + latest + " have aired" : "Episode out of range");
        }
        follow.setWatchedEpisode(watchedEpisode);
        follow.setProgressUpdatedAt(now);
        favoriteRepository.save(follow);
        if (aniListSync != null) {
            aniListSync.progressed(userId, mediaId, watchedEpisode, total);
        }
        return view(follow, total, now);
    }

    /**
     * My Schedule: the user's followed shows airing on {@code day} in the viewer's timezone, using
     * the same windows as the public weekly schedule.
     */
    @Transactional(readOnly = true)
    public List<FollowView> airingOn(long userId, DayOfWeek day, ZoneId zone) {
        Instant now = clock.instant();
        Instant bufferEnd = now.plus(bufferDays, ChronoUnit.DAYS);
        List<AiringDayWindows.Window> windows = AiringDayWindows.forDay(day, zone, now.minus(7, ChronoUnit.DAYS), bufferEnd);
        return favoriteRepository.findFollowsWithMedia(userId).stream()
                .filter(f -> {
                    MediaEntity m = f.getMedia();
                    Instant at = m.getNext_Airing_At();
                    return SCHEDULED_STATUSES.contains(m.getStatus()) && at != null && !at.isAfter(bufferEnd)
                            && windows.stream().anyMatch(w -> w.contains(at));
                })
                .sorted(Comparator.comparing((FavoriteMediaEntity f) -> f.getMedia().getNext_Airing_At()))
                .map(f -> view(f, null, now))
                .toList();
    }

    private FollowView view(FavoriteMediaEntity follow) {
        return view(follow, totalEpisodes(List.of(follow)).get(follow.getMedia().getId()), clock.instant());
    }

    private static FollowView view(FavoriteMediaEntity follow, Integer totalEpisodes, Instant now) {
        Integer latest = EpisodeProgress.latestAired(follow.getMedia(), totalEpisodes, now);
        int watched = follow.getWatchedEpisode();
        return new FollowView(follow.getMedia(), watched, latest, EpisodeProgress.behind(latest, watched),
                follow.getAddedDate(), follow.getProgressUpdatedAt());
    }

    /** Episode counts from media_info, needed only for finished shows (airing ones use next episode). */
    private Map<Long, Integer> totalEpisodes(List<FavoriteMediaEntity> follows) {
        List<Long> finishedIds = follows.stream()
                .map(FavoriteMediaEntity::getMedia)
                .filter(m -> m.getNext_Airing_Episode() == null && "FINISHED".equals(m.getStatus()))
                .map(MediaEntity::getId)
                .toList();
        if (finishedIds.isEmpty()) {
            return Map.of();
        }
        return mediaInfoRepository.findAllById(finishedIds).stream()
                .filter(i -> i.getTotalepisodes() != null)
                .collect(Collectors.toMap(MediaInfoEntity::getId, MediaInfoEntity::getTotalepisodes));
    }
}
