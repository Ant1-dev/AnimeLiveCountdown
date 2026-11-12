package com.Toine.animeCountdownBackend.controllers;

import com.Toine.animeCountdownBackend.components.CurrentUser;
import com.Toine.animeCountdownBackend.models.postgreEntities.FavoriteMediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.MediaEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.ReminderEntity;
import com.Toine.animeCountdownBackend.models.postgreEntities.UserEntity;
import com.Toine.animeCountdownBackend.repositories.FavoriteMediaRepository;
import com.Toine.animeCountdownBackend.repositories.MediaRepository;
import com.Toine.animeCountdownBackend.repositories.ReminderRepository;
import com.Toine.animeCountdownBackend.repositories.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    public record ReminderRequest(Integer leadMinutes, Boolean push, Boolean discord) {
    }

    public record ReminderDto(Long mediaId, int leadMinutes, boolean push, boolean discord, boolean following,
                              String title, String coverImageUrl, Instant nextAiringAt, Integer nextEpisode) {
    }

    private final ReminderRepository reminderRepository;
    private final FavoriteMediaRepository favoriteMediaRepository;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;

    public ReminderController(ReminderRepository reminderRepository, FavoriteMediaRepository favoriteMediaRepository,
                              MediaRepository mediaRepository, UserRepository userRepository) {
        this.reminderRepository = reminderRepository;
        this.favoriteMediaRepository = favoriteMediaRepository;
        this.mediaRepository = mediaRepository;
        this.userRepository = userRepository;
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
        return ResponseEntity.ok(reminders.stream()
                .map(r -> toDto(r, media.get(r.getMediaId()), following.contains(r.getMediaId())))
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
                        favoriteMediaRepository.existsByUserIdAndMediaId(user.getId(), mediaId))))
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
        try {
            reminder = reminderRepository.save(reminder);
        } catch (DataIntegrityViolationException e) {
            // Two saves raced; the other one won and holds the same preference
            reminder = reminderRepository.findByUser_IdAndMediaId(user.getId(), mediaId).orElseThrow();
        }
        return ResponseEntity.ok(toDto(reminder, media.get(), true));
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

    private static ReminderDto toDto(ReminderEntity r, MediaEntity media, boolean following) {
        String title = null;
        if (media != null) {
            title = media.getTitle_English() != null && !media.getTitle_English().isBlank()
                    ? media.getTitle_English() : media.getTitle_Romaji();
        }
        return new ReminderDto(r.getMediaId(), r.getLeadMinutes(), r.isPushEnabled(), r.isDiscordEnabled(), following,
                title, media != null ? media.getCover_Image_Url() : null,
                media != null ? media.getNext_Airing_At() : null,
                media != null ? media.getNext_Airing_Episode() : null);
    }
}
