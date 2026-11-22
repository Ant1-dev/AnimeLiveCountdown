package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.components.CurrentUser;
import com.Toine.animeCountdownBackend.models.postgreEntities.FavoriteMediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.ReminderEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.FavoriteMediaRepository;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import com.Toine.animeCountdownBackend.repositories.ReleaseTimeRepository;
import com.Toine.animeCountdownBackend.models.postgreEntities.ReleaseTimeEntity;
import com.Toine.animeCountdownBackend.repositories.ReminderRepository;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Per-show reminder preferences. Reminders apply to followed shows, so saving one also adds the
 * show to the user's favorites.
 */
@RestController
@RequestMapping("/api/auth/reminders")
public class ReminderController {

    /** releaseType: JAPAN (default), SUB or DUB; omitted keeps the current choice. */
    public record ReminderRequest(Integer leadMinutes, Boolean push, Boolean discord, String releaseType) {
        public ReminderRequest(Integer leadMinutes, Boolean push, Boolean discord) {
            this(leadMinutes, push, discord, null);
        }
    }

    /**
     * nextAiringAt/nextEpisode are for the reminder's release type (the Japanese airing, or the next
     * sub/dub release). availableTypes lists the release types the show has times for.
     */
    public record ReminderDto(Long mediaId, int leadMinutes, boolean push, boolean discord, boolean following,
                              String title, String coverImageUrl, Instant nextAiringAt, Integer nextEpisode,
                              String releaseType, List<String> availableTypes) {
    }

    private final ReminderRepository reminderRepository;
    private final FavoriteMediaRepository favoriteMediaRepository;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final ReleaseTimeRepository releaseTimeRepository;

    public ReminderController(ReminderRepository reminderRepository, FavoriteMediaRepository favoriteMediaRepository,
                              MediaRepository mediaRepository, UserRepository userRepository,
                              ReleaseTimeRepository releaseTimeRepository) {
        this.reminderRepository = reminderRepository;
        this.favoriteMediaRepository = favoriteMediaRepository;
        this.mediaRepository = mediaRepository;
        this.userRepository = userRepository;
        this.releaseTimeRepository = releaseTimeRepository;
    }

    @GetMapping
    public ResponseEntity<List<ReminderDto>> list() {
        UserEntity user = CurrentUser.get();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        List<ReminderEntity> reminders = reminderRepository.findByUser_IdOrderByCreatedAtDesc(user.getId());
        Map<Long, MediaEntity> media = mediaRepository.findAllById(reminders.stream().map(ReminderEntity::getMediaId).toList())
                .stream().collect(Collectors.toMap(MediaEntity::getId, Function.identity()));
        Set<Long> following = favoriteMediaRepository.findAllFavMedia(user.getId()).stream()
                .map(MediaEntity::getId).collect(Collectors.toSet());
        Map<Long, List<String>> types = availableTypes(reminders.stream().map(ReminderEntity::getMediaId).toList());
        return ResponseEntity.ok(reminders.stream()
                .map(r -> toDto(r, media.get(r.getMediaId()), following.contains(r.getMediaId()), types))
                .toList());
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<ReminderDto> get(@PathVariable Long mediaId) {
        UserEntity user = CurrentUser.get();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return reminderRepository.findByUser_IdAndMediaId(user.getId(), mediaId)
                .map(r -> ResponseEntity.ok(toDto(r, mediaRepository.findById(mediaId).orElse(null),
                        favoriteMediaRepository.existsByUserIdAndMediaId(user.getId(), mediaId),
                        availableTypes(List.of(mediaId)))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{mediaId}")
    public ResponseEntity<?> save(@PathVariable Long mediaId, @RequestBody ReminderRequest request) {
        UserEntity user = CurrentUser.get();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        int lead = request.leadMinutes() == null ? 0 : request.leadMinutes();
        if (lead < 0 || lead > ReminderEntity.MAX_LEAD_MINUTES) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lead time must be between 0 and "
                    + ReminderEntity.MAX_LEAD_MINUTES + " minutes"));
        }
        String releaseType = request.releaseType() == null ? null : request.releaseType().trim().toUpperCase(Locale.ROOT);
        if (releaseType != null && !ReminderEntity.RELEASE_TYPES.contains(releaseType)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Release type must be JAPAN, SUB or DUB"));
        }
        Optional<MediaEntity> media = mediaRepository.findById(mediaId);
        if (media.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Show not found"));
        }

        UserEntity userRef = userRepository.getReferenceById(user.getId());
        ensureFollowing(userRef, media.get());

        ReminderEntity reminder = reminderRepository.findByUser_IdAndMediaId(user.getId(), mediaId).orElseGet(() -> {
            ReminderEntity created = new ReminderEntity();
            created.setUser(userRef);
            created.setMediaId(mediaId);
            return created;
        });
        reminder.setLeadMinutes(lead);
        reminder.setPushEnabled(request.push() == null || request.push());
        reminder.setDiscordEnabled(request.discord() != null && request.discord());
        if (releaseType != null) {
            reminder.setReleaseType(releaseType);
        }
        try {
            reminder = reminderRepository.save(reminder);
        } catch (DataIntegrityViolationException e) {
            // Two saves raced; the other one won and holds the same preference
            reminder = reminderRepository.findByUser_IdAndMediaId(user.getId(), mediaId).orElseThrow();
        }
        return ResponseEntity.ok(toDto(reminder, media.get(), true, availableTypes(List.of(mediaId))));
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> delete(@PathVariable Long mediaId) {
        UserEntity user = CurrentUser.get();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        reminderRepository.deleteByUserAndMedia(user.getId(), mediaId);
        return ResponseEntity.noContent().build();
    }

    private void ensureFollowing(UserEntity user, MediaEntity media) {
        if (favoriteMediaRepository.existsByUserIdAndMediaId(user.getId(), media.getId())) {
            return;
        }
        FavoriteMediaEntity favorite = new FavoriteMediaEntity();
        favorite.setUser(user);
        favorite.setMedia(media);
        favorite.setAddedDate(Instant.now());
        try {
            favoriteMediaRepository.save(favorite);
        } catch (DataIntegrityViolationException ignored) {
            // Already followed (concurrent request)
        }
    }

    /** Per show, the release types it has times for: always JAPAN, plus SUB/DUB when synced. */
    private Map<Long, List<String>> availableTypes(Collection<Long> mediaIds) {
        Map<Long, Set<String>> found = new HashMap<>();
        if (!mediaIds.isEmpty()) {
            for (Object[] row : releaseTimeRepository.findAirTypes(mediaIds)) {
                found.computeIfAbsent((Long) row[0], k -> new HashSet<>()).add((String) row[1]);
            }
        }
        Map<Long, List<String>> out = new HashMap<>();
        for (Long id : mediaIds) {
            Set<String> types = found.getOrDefault(id, Set.of());
            List<String> list = new ArrayList<>(List.of(ReminderEntity.TYPE_JAPAN));
            for (String t : List.of(ReminderEntity.TYPE_SUB, ReminderEntity.TYPE_DUB)) {
                if (types.contains(t)) {
                    list.add(t);
                }
            }
            out.put(id, list);
        }
        return out;
    }

    private ReminderDto toDto(ReminderEntity r, MediaEntity media, boolean following, Map<Long, List<String>> types) {
        String title = null;
        if (media != null) {
            title = media.getTitle_English() != null && !media.getTitle_English().isBlank()
                    ? media.getTitle_English() : media.getTitle_Romaji();
        }
        Instant nextAt = media != null ? media.getNext_Airing_At() : null;
        Integer nextEpisode = media != null ? media.getNext_Airing_Episode() : null;
        String type = r.getReleaseType() == null ? ReminderEntity.TYPE_JAPAN : r.getReleaseType();
        if (!ReminderEntity.TYPE_JAPAN.equals(type)) {
            // The next sub/dub release (one that came out in the last hour still counts as next)
            Instant since = Instant.now().minus(Duration.ofHours(1));
            ReleaseTimeEntity next = releaseTimeRepository.findUpcoming(r.getMediaId(), type, since).stream()
                    .min(Comparator.comparing(ReminderController::releasedAt)).orElse(null);
            nextAt = next == null ? null : releasedAt(next);
            nextEpisode = next == null ? null : next.getEpisode();
        }
        return new ReminderDto(r.getMediaId(), r.getLeadMinutes(), r.isPushEnabled(), r.isDiscordEnabled(), following,
                title, media != null ? media.getCover_Image_Url() : null, nextAt, nextEpisode, type,
                types.getOrDefault(r.getMediaId(), List.of(ReminderEntity.TYPE_JAPAN)));
    }

    /** When a release is actually out: its delayed_until if it was pushed back. */
    private static Instant releasedAt(ReleaseTimeEntity r) {
        return r.getDelayedUntil() != null && r.getDelayedUntil().isAfter(r.getReleaseAt()) ? r.getDelayedUntil() : r.getReleaseAt();
    }
}
